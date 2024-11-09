package com.unbidden.telegramcoursesbot.service.payment;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.dao.ImageDao;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.RefundImpossibleException;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.PaymentDetails;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.PaymentDetailsRepository;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery.AnswerPreCheckoutQueryBuilder;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice.SendInvoiceBuilder;
import org.telegram.telegrambots.meta.api.methods.payments.RefundStarPayment;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.api.objects.payments.SuccessfulPayment;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class PaymentServiceImpl implements PaymentService {
    private static final Logger LOGGER = LogManager.getLogger(PaymentServiceImpl.class);

    private static final String PARAM_CURRENT_PRICE = "${currentPrice}";
    private static final String PARAM_COURSE_NAME = "${courseName}";
    private static final String PARAM_MAX_STAGE_FOR_REFUND = "${maxStageForRefund}";
    private static final String PARAM_CURRENT_STAGE = "${currentStage}";

    private static final String SERVICE_REFUND_SUCCESS = "service_refund_success";
    private static final String SERVICE_SUCCESSFUL_PAYMENT = "service_successful_payment";

    private static final String ERROR_PRE_CHECKOUT_PRICE_MISMATCH =
            "error_pre_checkout_price_mismatch";
    private static final String ERROR_PRE_CHECKOUT_CURRENCY_MISMATCH =
            "error_pre_checkout_currency_mismatch";
    private static final String ERROR_PAYMENT_DETAILS_NOT_FOUND =
            "error_payment_details_not_found";
    private static final String ERROR_PRECHECKOUT_UNKNOWN_COURSE =
            "error_precheckout_unknown_course";
    private static final String ERROR_PRE_CHECKOUT_COURSE_ALREADY_OWNED =
            "error_pre_checkout_course_already_owned";
    private static final String ERROR_ANSWER_PRECHECKOUT_FAILURE =
            "error_answer_precheckout_failure";
    private static final String ERROR_SEND_INVOICE_FAILURE = "error_send_invoice_failure";
    private static final String ERROR_REFUND_FAILURE = "error_refund_failure";
    private static final String ERROR_REFUND_USER_ADVANCED_TOO_FAR =
            "error_refund_user_advanced_too_far";
    private static final String ERROR_REFUND_COURSE_NOT_OWNED = "error_refund_course_not_owned";
    private static final String ERROR_REFUND_COURSE_WAS_GIFTED = "error_refund_course_was_gifted";
    private static final String ERROR_REFUND_COURSE_COMPLETED = "error_refund_course_completed";
    private static final String ERROR_REFUND_COURSE_UNAVAILABLE =
            "error_refund_course_unavailable";

    private static final String COURSE_INVOICE_DESCRIPTION = "course_%s_invoice_description";
    private static final String COURSE_INVOICE_TITLE = "course_%s_invoice_title";

    private static final String INVOICE_IMAGES_ENDPOINT = "/invoiceimages";

    private static final String TELEGRAM_STARS = "XTR";

    private static final String PROVIDER_TOKEN = "foo";

    @Autowired
    private PaymentDetailsRepository paymentDetailsRepository;

    @Autowired
    private ImageDao imageDao;

    @Autowired
    @Lazy
    private CourseService courseService;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomTelegramClient client;

    @Autowired
    private LocalizationLoader localizationLoader;

    @Value("${telegram.bot.webhook.url}")
    private String serverUrl;

    @Override
    public boolean isAvailable(@NonNull User user, @NonNull String courseName) {
        return isAvailable0(userService.getUser(user.getId()), courseName);
    }

    @Override
    public boolean isAvailable(@NonNull UserEntity user, @NonNull String courseName) {
        return isAvailable0(user, courseName);
    }

    @Override
    public boolean isAvailableAndGifted(@NonNull UserEntity user, @NonNull String courseName) {
        final Course course = courseService.getCourseByName(courseName, user);
        
        return !paymentDetailsRepository
                .findByUserIdAndCourseName(user.getId(), course.getName()).stream()
                .filter(pd -> pd.isGifted())
                .toList()
                .isEmpty();
    }

    @Override
    @NonNull
    public PaymentDetails addPaymentDetails(@NonNull PaymentDetails paymentDetails) {
        return paymentDetailsRepository.save(paymentDetails);
    }

    @Override
    public void deleteByCourseForUser(@NonNull String courseName, @NonNull Long userId) {
        PaymentDetails details = paymentDetailsRepository
                .findByUserIdAndCourseName(userId, courseName).get(0);
        paymentDetailsRepository.delete(details);
    }

    @Override
    public void sendInvoice(@NonNull UserEntity user, @NonNull String courseName) {
        final Course course = courseService.getCourseByName(courseName, user);
        final String imageUrl = serverUrl + INVOICE_IMAGES_ENDPOINT + "/" + courseName;

        LOGGER.debug("Compiling invoce for course " + courseName + " for user " + user.getId()
                + "...", imageUrl);
        SendInvoiceBuilder<?, ?> builder = SendInvoice.builder()
                .chatId(user.getId())
                .title(localizationLoader.getLocalizationForUser(COURSE_INVOICE_TITLE
                    .formatted(courseName), user).getData())
                .description(localizationLoader.getLocalizationForUser(COURSE_INVOICE_DESCRIPTION
                    .formatted(courseName), user).getData())
                .payload(course.getName())
                .providerToken(PROVIDER_TOKEN)
                .currency(TELEGRAM_STARS)
                .price(LabeledPrice.builder()
                    .amount(course.getPrice())
                    .label(courseName)
                    .build())
                .startParameter(course.getName());

                if (imageDao.isPresent(courseName)) {
                    builder.photoUrl(imageUrl);
                } else {
                    LOGGER.warn("Image for invoice for course " + courseName
                            + " is not available.");
                }
        try {
            LOGGER.debug("Sending invoice for course " + course.getName()
                    + " to user " + user.getId() + "...");
                    client.execute(builder.build());
            LOGGER.debug("Invoice sent.");
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send invoice for "
                    + course.getName() + " to user " + user.getId(), localizationLoader
                    .getLocalizationForUser(ERROR_SEND_INVOICE_FAILURE, user), e);
        }
    }

    @Override
    public void resolvePreCheckout(@NonNull PreCheckoutQuery preCheckoutQuery) {
        final AnswerPreCheckoutQueryBuilder<?, ?> answerBuilder =
                AnswerPreCheckoutQuery.builder()
                    .preCheckoutQueryId(preCheckoutQuery.getId())
                    .ok(false);
        final UserEntity user = userService.getUser(preCheckoutQuery.getFrom().getId());

        try {
            final Course course = courseService.getCourseByName(
                    preCheckoutQuery.getInvoicePayload(), user);
            
            LOGGER.info("Precheckout query was sent for course " + course.getName()
                    + " by user " + user.getId() + ".");
            
            if (isAvailable(user, course.getName())) {
                final Localization errorLoc = localizationLoader.getLocalizationForUser(
                        ERROR_PRE_CHECKOUT_COURSE_ALREADY_OWNED, user, PARAM_COURSE_NAME,
                        course.getName());
                answerBuilder.errorMessage(errorLoc.getData());
                LOGGER.warn("Precheckout failed: user " + user.getId()
                        + " already has course " + course.getName());
                client.sendMessage(user, errorLoc);
                
            } else if (!preCheckoutQuery.getCurrency().equals(TELEGRAM_STARS)) {
                final Localization errorLoc = localizationLoader.getLocalizationForUser(
                        ERROR_PRE_CHECKOUT_CURRENCY_MISMATCH, user);
                answerBuilder.errorMessage(errorLoc.getData());
                LOGGER.error("Precheckout failed: currency mismatch. Investigation required. "
                        + "User: " + user.getId() + ", course: " + course.getName());
                client.sendMessage(user, errorLoc);
            } else if (!preCheckoutQuery.getTotalAmount().equals(course.getPrice())) {
                final Localization errorLoc = localizationLoader.getLocalizationForUser(
                        ERROR_PRE_CHECKOUT_PRICE_MISMATCH, user, PARAM_CURRENT_PRICE,
                        course.getPrice());
                answerBuilder.errorMessage(errorLoc.getData());
                LOGGER.warn("Precheckout failed: User " + user.getId()
                        + " is using invoice with price " + preCheckoutQuery.getTotalAmount()
                        + " while course " + course.getName() + "'s current price is "
                        + course.getPrice());
                client.sendMessage(user, errorLoc);
            } else {
                LOGGER.info("Precheckout completed for course " + course.getName()
                        + " and user " + user.getId() + ".");
                answerBuilder.ok(true);
            }
        } catch (EntityNotFoundException e) {
            final Localization errorLoc = localizationLoader.getLocalizationForUser(
                    ERROR_PRECHECKOUT_UNKNOWN_COURSE, user, PARAM_COURSE_NAME,
                    preCheckoutQuery.getInvoicePayload());
            answerBuilder.errorMessage(errorLoc.getData());
            LOGGER.error("Precheckout query payload contained unknown course: "
                    + preCheckoutQuery.getInvoicePayload() + ". Investigation required. User "
                    + user.getId());
            client.sendMessage(user, errorLoc);
        }

        try {
            LOGGER.info("Sending precheckout response...");
            client.execute(answerBuilder.build());
            LOGGER.info("Precheckout response sent.");
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to answer precheckout query",
                    localizationLoader.getLocalizationForUser(ERROR_ANSWER_PRECHECKOUT_FAILURE,
                    preCheckoutQuery.getFrom()), e);
        }
    }

    @Override
    public void resolveSuccessfulPayment(@NonNull Message message) {
        final UserEntity user = userService.getUser(message.getFrom().getId());
        final SuccessfulPayment payment = message.getSuccessfulPayment();

        final PaymentDetails paymentDetails = new PaymentDetails();
        paymentDetails.setTelegramPaymentChargeId(payment.getTelegramPaymentChargeId());
        paymentDetails.setUser(user);
        paymentDetails.setTotalAmount(payment.getTotalAmount());
        paymentDetails.setValid(true);
        paymentDetails.setGifted(false);
        paymentDetails.setTimestamp(LocalDateTime.now());

        final Course course = courseService.getCourseByName(payment.getInvoicePayload(), user);

        LOGGER.info("Successful payment was sent for course " + course.getName()
                + " by user " + user.getId() + ".");
        paymentDetails.setCourse(course);

        LOGGER.debug("Saving payment details...");
        addPaymentDetails(paymentDetails);
        LOGGER.debug("Payment details saved. Sending confirmation message...");
        client.sendMessage(user, localizationLoader.getLocalizationForUser(
                SERVICE_SUCCESSFUL_PAYMENT, user, PARAM_COURSE_NAME, course.getName()));
        LOGGER.debug("Message sent.");
        LOGGER.info("User " + user.getId() + " has bought course " + course.getName()
                + "! Do not forget to thank them!");
        LOGGER.debug("Initiating course " + course.getName() + " for user "
                + user.getId() + "...");
        courseService.initMessage(user, course.getName());
        LOGGER.debug("Course initiated.");
    }

    @Override
    public void refund(@NonNull UserEntity user, @NonNull String courseName) {
        final List<PaymentDetails> paymentDetailsList = paymentDetailsRepository
                .findByUserIdAndCourseName(user.getId(), courseName);

        for (PaymentDetails paymentDetailsEntry : paymentDetailsList) {
            if (!paymentDetailsEntry.isGifted() && paymentDetailsEntry.isValid()) {
                final RefundStarPayment refundStarPayment = RefundStarPayment.builder()
                        .telegramPaymentChargeId(paymentDetailsEntry.getTelegramPaymentChargeId())
                        .userId(user.getId())
                        .build();
                try {
                    client.execute(refundStarPayment);
                } catch (TelegramApiException e) {
                    throw new TelegramException(courseName, localizationLoader.getLocalizationForUser(ERROR_REFUND_FAILURE, user), e);
                }
                paymentDetailsEntry.setRefundedAt(LocalDateTime.now());
                paymentDetailsEntry.setValid(false);
                LOGGER.debug("Invalidating payment details for course " + courseName
                        + " and user " + user.getId() + "..."); 
                paymentDetailsRepository.save(paymentDetailsEntry);
                LOGGER.info("Payment details " + paymentDetailsEntry.getId()
                        + " has been invalidated.");
                LOGGER.debug("Sending confirmation message...");
                client.sendMessage(user, localizationLoader.getLocalizationForUser(
                        SERVICE_REFUND_SUCCESS, user, PARAM_COURSE_NAME, courseName));
                LOGGER.debug("Message sent.");
                return;
            }
        }
        throw new EntityNotFoundException("Unable to refund course " + courseName
                + " for user " + user.getId() + " because there is no valid payment "
                + "details present", localizationLoader.getLocalizationForUser(
                ERROR_PAYMENT_DETAILS_NOT_FOUND, user));
    }

    @Override
    @NonNull
    public List<PaymentDetails> getAllForUser(@NonNull UserEntity user) {
        return paymentDetailsRepository.findByUser(user.getId());
    }

    @Override
    public boolean isRefundPossible(@NonNull UserEntity user, @NonNull String courseName) {
        final Course course = courseService.getCourseByName(courseName, user);
        LOGGER.debug("Performing checks...");
        if (!isAvailable(user, course.getName())) {
            throw new RefundImpossibleException("Course " + course.getName()
                    + " is not owned by user " + user.getId(), localizationLoader
                    .getLocalizationForUser(ERROR_REFUND_COURSE_NOT_OWNED, user));
        }
        if (isAvailableAndGifted(user, course.getName())) {
            throw new RefundImpossibleException("Course " + course.getName()
                    + " was gifted to user " + user.getId()
                    + " and therefore it cannot be refunded", localizationLoader
                    .getLocalizationForUser(ERROR_REFUND_COURSE_WAS_GIFTED, user));
        }
        if (course.getRefundStage() < 0) {
            throw new RefundImpossibleException("Refund of course " + courseName 
                    + " is not possible", localizationLoader.getLocalizationForUser(
                    ERROR_REFUND_COURSE_UNAVAILABLE, user, PARAM_COURSE_NAME, courseName));
        }
        final CourseProgress courseProgress = courseService
                .getCurrentCourseProgressForUser(user.getId(), courseName);
        if (courseProgress.getNumberOfTimesCompleted() > 0) {
            throw new RefundImpossibleException("User " + user.getId() + " cannot refund course "
                    + course.getName() + " because they have already completed it",
                    localizationLoader.getLocalizationForUser(ERROR_REFUND_COURSE_COMPLETED,
                    user, PARAM_COURSE_NAME, course.getName()));
        }
        if (courseProgress.getStage() > course.getRefundStage()) {
            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put(PARAM_COURSE_NAME, course.getName());
            parameterMap.put(PARAM_CURRENT_STAGE, courseProgress.getStage());
            parameterMap.put(PARAM_MAX_STAGE_FOR_REFUND, course.getRefundStage());

            throw new RefundImpossibleException("User " + user.getId()
                    + " has advanced in course " + course.getName() + " to "
                    + courseProgress.getStage() + " lesson which is past lesson "
                    + course.getRefundStage() + " and therefore refund is now impossible",
                    localizationLoader.getLocalizationForUser(ERROR_REFUND_USER_ADVANCED_TOO_FAR,
                    user, parameterMap));
        }
        LOGGER.debug("User " + user.getId() + " is eligible for course "
                + course.getName() + "'s refund.");
        return true;
    }

    private boolean isAvailable0(UserEntity user, String courseName) {
        final Course course = courseService.getCourseByName(courseName, user);
        
        return !paymentDetailsRepository.findByUserIdAndCourseName(user.getId(), course.getName())
                .isEmpty();
    }
}
