package com.unbidden.telegramcoursesbot.service.orchestration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery.AnswerPreCheckoutQueryBuilder;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice.SendInvoiceBuilder;
import org.telegram.telegrambots.meta.api.methods.payments.RefundStarPayment;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.api.objects.payments.SuccessfulPayment;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dao.ImageDao;
import com.unbidden.telegramcoursesbot.exception.CourseIsAlreadyOwnedException;
import com.unbidden.telegramcoursesbot.exception.OnMaintenanceException;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseOwnership;
import com.unbidden.telegramcoursesbot.model.CurrencyCode;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.Course.PaymentType;
import com.unbidden.telegramcoursesbot.model.TelegramPaymentDetails;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService.PreCheckoutResponse;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(PaymentOrchestrationService.class);

    private static final String EXTERNAL_INVOICE_MENU = "m_extInv";

    private static final String PARAM_CURRENT_PRICE = "${currentPrice}";
    private static final String PARAM_COURSE_NAME = "${courseName}";
    private static final String PARAM_COURSE_ID = "${courseId}";
    private static final String PARAM_USER_ID = "${userId}";
    private static final String PARAM_USER_FULL_NAME = "${userFullName}";
    private static final String PARAM_TARGET_TITLE = "${targetTitle}";
    private static final String PARAM_TARGET_FULL_NAME = "${targetFullName}";
    private static final String PARAM_SENDER_TITLE = "${senderTitle}";
    private static final String PARAM_SENDER_FULL_NAME = "${senderFullName}";

    private static final String SERVICE_REFUND_SUCCESS = "service_refund_success";
    private static final String SERVICE_USER_REFUNDED_COURSE = "service_user_refunded_course";
    private static final String SERVICE_COURSE_EXTERNAL_INVOICE_MEDIA_GROUP_BYPASS = "service_course_external_invoice_media_group_bypass";
    private static final String SERVICE_SUCCESSFUL_PAYMENT = "service_successful_payment";
    private static final String SERVICE_USER_BOUGHT_COURSE = "service_user_bought_course";
    private static final String SERVICE_AUTOMATIC_REFUND_NOTIFICATION = "service_automatic_refund_notification";
    private static final String SERVICE_COURSE_GIFTED_SUCCESSFULY = "service_course_gifted_successfuly";
    private static final String SERVICE_COURSE_GIFTED_NOTIFICATION = "service_course_gifted_notification";

    private static final String ERROR_SEND_INVOICE_FAILURE = "error_send_invoice_failure";
    private static final String ERROR_PRE_CHECKOUT_PRICE_MISMATCH = "error_pre_checkout_price_mismatch";
    private static final String ERROR_PRE_CHECKOUT_CURRENCY_MISMATCH = "error_pre_checkout_currency_mismatch";
    private static final String ERROR_PRE_CHECKOUT_UNKNOWN_COURSE = "error_pre_checkout_unknown_course";
    private static final String ERROR_PRE_CHECKOUT_COURSE_ALREADY_OWNED = "error_pre_checkout_course_already_owned";
    private static final String ERROR_ANSWER_PRECHECKOUT_FAILURE = "error_answer_precheckout_failure";
    private static final String ERROR_PAYMENT_SUCCESS_SERVER_ON_MAINTENANCE = "error_payment_success_server_on_maintenance";
    private static final String ERROR_REFUND_FAILURE = "error_refund_failure";
    private static final String ERROR_AUTOMATIC_REFUND_FAILURE = "error_automatic_refund_failure";
    private static final String ERROR_AUTOMATIC_REFUND_FAILURE_NOTIFICATION = "error_automatic_refund_failure_notification";

    private static final String INVOICE_IMAGES_ENDPOINT = "/invoiceimages";

    private static final String PROVIDER_TOKEN = "foo";
    
    private final ImageDao imageDao;

    private final PaymentService paymentService;

    private final ContentService contentService;

    private final CourseOrchestrationService courseService;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    private final EntityUtil entityUtil;

    private final ClientManager clientManager;

    @Value("${telegram.bot.webhook.url}")
    private String serverUrl;

    public boolean isAvailable(UserEntity user, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        return paymentService.isAvailable(user, courseId);
    }

    public boolean isAvailableAndGifted(UserEntity user, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        return paymentService.isAvailableAndGifted(user, courseId);
    }

    public void sendInvoice(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = entityUtil.getCourseById(user, bot, courseId);
        
        if (course.getPaymentType() == PaymentType.TELEGRAM) {
            sendTelegramInvoice(user, bot, course);
        } else {
            sendExternalInvoice(user, bot, course);
        }
    }

    public void resolvePreCheckout(UserEntity user, Bot bot, PreCheckoutQuery preCheckoutQuery) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(preCheckoutQuery, "preCheckoutQuery cannot be null");

        final AnswerPreCheckoutQueryBuilder<?, ?> answerBuilder =
                AnswerPreCheckoutQuery.builder()
                    .preCheckoutQueryId(preCheckoutQuery.getId())
                    .ok(false);
        final PreCheckoutResponse response = paymentService.checkPreCheckoutConditions(user, bot, preCheckoutQuery);

        LOGGER.info("Precheckout query was sent for course " + response.course().getId() + " by user " + user.getId() + ".");

        Localization errorLoc = null;
        switch (response.result()) {
            case ALREADY_OWNED -> {
                errorLoc = localizationLoader.getLocalizationForUser(
                        ERROR_PRE_CHECKOUT_COURSE_ALREADY_OWNED, user, PARAM_COURSE_NAME,
                        contentService.getLocalizedText(user, bot, response.course().getTitle().getId()));

                LOGGER.info("Precheckout failed: user " + user.getId()
                        + " already has course " + response.course().getId());
            }
            case COURSE_NOT_FOUND -> {
                errorLoc = localizationLoader.getLocalizationForUser(
                        ERROR_PRE_CHECKOUT_UNKNOWN_COURSE, user);

                LOGGER.info("Precheckout query payload contained unknown course ID: "
                        + preCheckoutQuery.getInvoicePayload() + ". User: " + user.getId());
            }
            case CURRENCY_MISMATCH -> {
                errorLoc = localizationLoader.getLocalizationForUser(
                        ERROR_PRE_CHECKOUT_CURRENCY_MISMATCH, user);
                        
                LOGGER.error("Precheckout failed: currency mismatch. Investigation required. "
                        + "User: " + user.getId() + ", course: " + response.course().getId());
            }
            case PRICE_MISMATCH -> {
                errorLoc = localizationLoader.getLocalizationForUser(
                        ERROR_PRE_CHECKOUT_PRICE_MISMATCH, user, PARAM_CURRENT_PRICE,
                        response.course().getPrice());

                LOGGER.info("Precheckout failed: User " + user.getId()
                        + " used invoice with price " + preCheckoutQuery.getTotalAmount()
                        + " while course " + response.course().getId() + "'s current price is "
                        + response.course().getPrice());
            }
            default -> {
                answerBuilder.ok(true);
            }
        }

        if (errorLoc != null) {
            answerBuilder.errorMessage(errorLoc.getData());
            clientManager.getClient(bot).sendMessage(user, errorLoc);
        }

        try {
            LOGGER.debug("Sending precheckout response...");
            clientManager.getClient(bot).execute(answerBuilder.build());
            LOGGER.info("Precheckout completed for course " + response.course().getId()
                    + " and user " + user.getId() + ".");
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to answer precheckout query",
                    localizationLoader.getLocalizationForUser(ERROR_ANSWER_PRECHECKOUT_FAILURE,
                    user), e);
        }
    }

    public void resolveSuccessfulPayment(UserEntity user, Bot bot, SuccessfulPayment payment) {
        final UserEntity creator = entityUtil.getCreator(bot);

        LOGGER.info("Successful payment was sent for course " + payment.getInvoicePayload()
                + " by user " + user.getId() + ".");
        
        try {
            final TelegramPaymentDetails paymentDetails = paymentService.registerTelegramPurchase(user, bot, payment);

            LOGGER.info("User " + user.getId() + " has bought course " + payment.getInvoicePayload()
                    + ". Payment details saved. Sending confirmation messages...");
        
            final Map<String, Object> parameterMapUser = new HashMap<>();

            parameterMapUser.put(PARAM_COURSE_NAME, contentService.getLocalizedText(user, bot, paymentDetails.getCourse().getTitle().getId()));
            parameterMapUser.put(PARAM_USER_FULL_NAME, user.getFullName());
            final Map<String, Object> parameterMapCreator = new HashMap<>();

            parameterMapCreator.put(PARAM_COURSE_NAME, contentService.getLocalizedText(creator, bot, paymentDetails.getCourse().getTitle().getId()));
            parameterMapCreator.put(PARAM_USER_FULL_NAME, user.getFullName());

            clientManager.getClient(bot).sendMessage(user, localizationLoader.getLocalizationForUser(
                    SERVICE_SUCCESSFUL_PAYMENT, user, parameterMapUser));
            clientManager.getClient(bot).sendMessage(creator, localizationLoader.getLocalizationForUser(
                    SERVICE_USER_BOUGHT_COURSE, creator, parameterMapCreator));
            LOGGER.debug("Messages sent.");
            if (clientManager.isOnMaintenance()) {
                throw new OnMaintenanceException("Unable to send course because server is on "
                        + "maintenance", localizationLoader.getLocalizationForUser(
                        ERROR_PAYMENT_SUCCESS_SERVER_ON_MAINTENANCE, user));
            }
            LOGGER.debug("Initiating course " + payment.getInvoicePayload() + " for user " + user.getId() + "...");
            courseService.initCourse(user, bot, paymentDetails.getCourse().getId());
            LOGGER.debug("Course initiated.");
        } catch (CourseIsAlreadyOwnedException e) {
            LOGGER.warn("Failed to process successfull payment due to course " + payment.getInvoicePayload()
                    + " already being owned by user " + user.getId() + ". Attempting refund...", e);
            
            final RefundStarPayment refundStarPayment = RefundStarPayment.builder()
                    .telegramPaymentChargeId(payment.getTelegramPaymentChargeId())
                    .userId(user.getId())
                    .build();
            final Map<String, Object> parameterMap = new HashMap<>();

            parameterMap.put(PARAM_COURSE_ID, payment.getInvoicePayload());
            parameterMap.put(PARAM_USER_ID, user.getId());

            try {
                LOGGER.debug("Executing automatic refund...");
                clientManager.getClient(bot).execute(refundStarPayment);
                clientManager.getClient(bot).sendMessage(creator, localizationLoader.getLocalizationForUser(
                        SERVICE_AUTOMATIC_REFUND_NOTIFICATION, creator, parameterMap));
                LOGGER.info("Automatic refund has been successfull and a notification has been sent to the Creator.");
            } catch (TelegramApiException e2) {
                clientManager.getClient(bot).sendMessage(creator, localizationLoader.getLocalizationForUser(
                        ERROR_AUTOMATIC_REFUND_FAILURE_NOTIFICATION, creator, parameterMap));
                throw new TelegramException("Failed to automatically refund user " + user.getId()
                        + " after a successfull Telegram payment failed due to the course already being owned.",
                        localizationLoader.getLocalizationForUser(ERROR_AUTOMATIC_REFUND_FAILURE, user), e2);
            }
        }
    }

    public CourseOwnership checkRefundPossible(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        return paymentService.checkRefundPossible(user, bot, courseId);
    }

    public void refund(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseOwnership ownership = paymentService.checkRefundPossible(user, bot, courseId);
        final TelegramPaymentDetails paymentDetails = (TelegramPaymentDetails)ownership.getLastPaymentDetails();
        final RefundStarPayment refundStarPayment = RefundStarPayment.builder()
                .telegramPaymentChargeId(paymentDetails.getTelegramPaymentChargeId())
                .userId(user.getId())
                .build();

        try {
            clientManager.getClient(bot).execute(refundStarPayment);
        } catch (TelegramApiException e) {
            throw new TelegramException("Failed to send a refund request to user "
                    + user.getId() + ".", localizationLoader
                    .getLocalizationForUser(ERROR_REFUND_FAILURE, user), e);
        }
        LOGGER.debug("Invalidating ownership for course " + courseId
                + " and user " + user.getId() + "..."); 
        paymentService.invalidateTelegramCourseOwnership(user, bot, courseId);
        LOGGER.info("Ownership " + ownership.getId() + " has been invalidated.");
        LOGGER.debug("Sending confirmation messages...");
        final Map<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(PARAM_COURSE_NAME, contentService.getLocalizedText(user, bot,
                entityUtil.getCourseById(user, bot, courseId).getTitle().getId()));
        parameterMap.put(PARAM_USER_FULL_NAME, user.getFullName());

        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_REFUND_SUCCESS, user, parameterMap));
        clientManager.getClient(bot).sendMessage(entityUtil.getCreator(bot),
                localizationLoader.getLocalizationForUser(SERVICE_USER_REFUNDED_COURSE,
                user, parameterMap));
        LOGGER.debug("Messages sent.");
    }

    public void giftCourse(UserEntity user, Bot bot, Long targetId, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        LOGGER.info("User " + user.getId() + " is trying to gift course " + courseId + " to user " + targetId + "...");
        final CourseOwnership ownership = paymentService.registerGiftCourse(user, bot, targetId, courseId);
        LOGGER.info("Course ownership " + ownership.getId() + " has been created/updated for user "
                + user.getId() + " and course " + courseId + ".");
        final Map<String, Object> paramMap = new HashMap<>();
        
        paramMap.put(PARAM_COURSE_NAME, contentService.getLocalizedText(user, bot, ownership.getCourse().getTitle().getId()));
        paramMap.put(PARAM_TARGET_FULL_NAME, ownership.getUser().getFullName());
        paramMap.put(PARAM_TARGET_TITLE, entityUtil.getLocalizedTitle(user, bot, ownership.getUser()));
        paramMap.put(PARAM_SENDER_FULL_NAME, user.getFullName());
        paramMap.put(PARAM_SENDER_TITLE, entityUtil.getLocalizedTitle(ownership.getUser(), bot, user));
        LOGGER.debug("Sending confirmation messages...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.getLocalizationForUser(
                SERVICE_COURSE_GIFTED_SUCCESSFULY, user, paramMap));
        clientManager.getClient(bot).sendMessage(ownership.getUser(), localizationLoader.getLocalizationForUser(
                SERVICE_COURSE_GIFTED_NOTIFICATION, user, paramMap));
        LOGGER.debug("Messages sent.");
    }

    private void sendTelegramInvoice(UserEntity user, Bot bot, Course course) {
        final String imageUrl = serverUrl + INVOICE_IMAGES_ENDPOINT + "/" + course.getId();
        final String courseName = contentService.getLocalizedText(user, bot, course.getTitle().getId());

        LOGGER.debug("Compiling invoice for course " + course.getId() + " for user " + user.getId() + "...");
        final SendInvoiceBuilder<?, ?> builder = SendInvoice.builder()
                .chatId(user.getId())
                .title(courseName)
                .description(course.getDescription() != null
                    ? contentService.getLocalizedText(user, bot, course.getDescription().getId())
                    : "") // TODO: potentially remove the not-null check
                .payload(course.getId().toString())
                .providerToken(PROVIDER_TOKEN)
                .currency(CurrencyCode.XTR.toString())
                .price(LabeledPrice.builder()
                    .amount(course.getPrice())
                    .label(courseName)
                    .build())
                .startParameter(course.getId().toString());

                if (imageDao.exists(course.getId())) {
                    builder.photoUrl(imageUrl);
                } else {
                    LOGGER.debug("Image for invoice for course " + course.getId() + " is not available.");
                }
        try {
            LOGGER.debug("Sending invoice for course " + course.getId()
                    + " to user " + user.getId() + "...");
            clientManager.getClient(bot).execute(builder.build());
            LOGGER.debug("Invoice sent.");
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send invoice for "
                    + course.getId() + " to user " + user.getId(), localizationLoader
                    .getLocalizationForUser(ERROR_SEND_INVOICE_FAILURE, user), e);
        }
    }

    private void sendExternalInvoice(UserEntity user, Bot bot, Course course) {
        LOGGER.debug("Sending an external invoice for course " + course.getId() + "...");

        final List<Message> sentMessages = contentService.sendLocalizedContent(
                user, bot, course.getExternalInvoice().getMapping().getId());

        final Message menuMessage;
        if (sentMessages.size() > 1) {
            LOGGER.debug("The content for course " + course.getId() + "'s external invoice is a media group.");

            menuMessage = clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_COURSE_EXTERNAL_INVOICE_MEDIA_GROUP_BYPASS, user));
            LOGGER.debug("Additional message for the menu has been sent.");
        } else {
            LOGGER.debug("The content for course " + course.getId() + "'s invoice is not a media group. "
                    + "The menu will be attached to the message.");    
            menuMessage = sentMessages.getFirst();
        }

        menuService.initiateMenu(user, bot, EXTERNAL_INVOICE_MENU, course.getExternalInvoice().getExternalStorePageUrl(),
                menuMessage.getMessageId());
        LOGGER.debug("External invoice menu has been sent.");
    }
}
