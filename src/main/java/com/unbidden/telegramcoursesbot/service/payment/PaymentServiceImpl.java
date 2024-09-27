package com.unbidden.telegramcoursesbot.service.payment;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.CourseModel;
import com.unbidden.telegramcoursesbot.model.PaymentDetails;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.repository.PaymentDetailsRepository;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.util.Photo;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery.AnswerPreCheckoutQueryBuilder;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.api.objects.payments.SuccessfulPayment;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

// TODO: accomodate for the new isGifted field.
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final Logger LOGGER = LogManager.getLogger(PaymentServiceImpl.class);

    private static final String DEFAULT_CURRENCY = "XTR";

    private static final String PROVIDER_TOKEN = "foo";

    private final PaymentDetailsRepository paymentDetailsRepository;

    private final CourseRepository courseRepository;

    private final TelegramBot bot;

    private final LocalizationLoader localizationLoader;

    private final TextUtil textUtil;

    @Override
    public boolean isAvailable(User user, String courseName) {
        return isAvailable0(user.getId(), courseName);
    }

    @Override
    public boolean isAvailable(UserEntity user, String courseName) {
        return isAvailable0(user.getId(), courseName);
    }

    @Override
    public boolean isAvailableAndGifted(UserEntity user, String courseName) {
        final CourseModel course = courseRepository.findByName(courseName).get();
        
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
    public void sendInvoice(User user, String courseName) {
        final CourseModel course = courseRepository.findByName(courseName).get();
        final Photo photo = textUtil.parsePhoto(localizationLoader
                .getLocalizationForUser(course.getLocFilePhotoName() + "invoice", user)
                .getData());

        SendInvoice sendInvoice = SendInvoice.builder()
                .chatId(user.getId())
                .title(localizationLoader.getLocalizationForUser(course.getLocFileInvoiceName()
                    + "title", user).getData())
                .description(localizationLoader.getLocalizationForUser(
                    course.getLocFileInvoiceName() + "description", user).getData())
                .payload(course.getName())
                .providerToken(PROVIDER_TOKEN)
                .currency(DEFAULT_CURRENCY)
                .price(LabeledPrice.builder()
                    .amount(course.getPrice())
                    .label("labeled_price")
                    .build())
                .startParameter(course.getName())
                .photoUrl(photo.getUrl())
                .photoSize(photo.getSize())
                .photoWidth(photo.getWidth())
                .photoHeight(photo.getHeight())
                .build();
        try {
            LOGGER.info("Sending invoice for course " + course.getName()
                    + " to user " + user.getId() + "...");
                    bot.execute(sendInvoice);
            LOGGER.info("Invoice sent.");
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send invoice for "
                    + course.getName() + " to user " + user.getId(), e);
        }
    }

    // TODO: Localizations are wrong. This needs fixing
    @Override
    public void resolvePreCheckout(PreCheckoutQuery preCheckoutQuery) {
        final AnswerPreCheckoutQueryBuilder answerBuilder =
                AnswerPreCheckoutQuery.builder()
                    .preCheckoutQueryId(preCheckoutQuery.getId())
                    .ok(false);
        final User user = preCheckoutQuery.getFrom();

        Optional<CourseModel> courseOpt = courseRepository.findByName(
                preCheckoutQuery.getInvoicePayload());
        if (courseOpt.isEmpty()) {
            answerBuilder.errorMessage(localizationLoader.getLocalizationForUser(
                    "error_precheckout_unknown_course", user).getData());
            LOGGER.error("Precheckout query payload contained unknown course: "
                    + preCheckoutQuery.getInvoicePayload() + ". Investigation required. User "
                    + user.getId());
            try {
                LOGGER.info("Sending precheckout response...");
                bot.execute(answerBuilder.build());
                LOGGER.info("Precheckout response sent.");
            } catch (TelegramApiException e) {
                throw new TelegramException("Unable to answer precheckout query with "
                        + "unknown course error.", e);
            }
        }

        final CourseModel course = courseOpt.get();

        LOGGER.info("Precheckout query was sent for course " + course.getName()
                + " by user " + user.getId() + ".");
        
        if (isAvailable(user, course.getName())) {
            answerBuilder.errorMessage(localizationLoader.getLocalizationForUser(
                    course.getLocFileErrorName()
                    + "pre_checkout_course_already_present", user).getData());
            LOGGER.warn("Precheckout failed: user " + user.getId()
                    + " already has this course.");
            
        } else if (!preCheckoutQuery.getCurrency().equals(DEFAULT_CURRENCY)) {
            answerBuilder.errorMessage(localizationLoader.getLocalizationForUser(
                    course.getLocFileErrorName()
                    + "pre_checkout_currency_mismatch", user).getData());
            LOGGER.error("Precheckout failed: currency mismatch. Investigation required. "
                    + "User: " + user.getId() + ", course: " + course.getName());
        } else if (preCheckoutQuery.getTotalAmount() != course.getPrice()) {
            answerBuilder.errorMessage(localizationLoader.getLocalizationForUser(
                    course.getLocFileErrorName()
                    + "pre_checkout_price_mismatch", user).getData());
            LOGGER.error("Precheckout failed: price mismatch. Investigation required. "
                    + "User: " + user.getId() + ", course: " + course.getName());
        } else {
            LOGGER.info("Precheckout completed for course " + course.getName()
                    + " and user " + user.getId() + ".");
            answerBuilder.ok(true);
        }

        try {
            LOGGER.info("Sending precheckout response...");
            bot.execute(answerBuilder.build());
            LOGGER.info("Precheckout response sent.");
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to answer precheckout query for course "
                    + course.getName() + " to user " + user.getId(), e);
        }
    }

    @Override
    public void resolveSuccessfulPayment(Message message) {
        final User user = message.getFrom();
        final PaymentDetails paymentDetails = new PaymentDetails();
        final SuccessfulPayment payment = message.getSuccessfulPayment();

        paymentDetails.setTelegramPaymentChargeId(payment.getTelegramPaymentChargeId());
        paymentDetails.setUser(new UserEntity(user));
        paymentDetails.setTotalAmount(payment.getTotalAmount());

        Optional<CourseModel> courseOpt = courseRepository.findByName(
                payment.getInvoicePayload());
        if (courseOpt.isEmpty()) {
            LOGGER.error("Successfull payment payload contained unknown course: "
                    + payment.getInvoicePayload()
                    + ". Investigation required. Automatic refund will be initiated. User "
                    + user.getId());
            paymentDetails.setSuccessful(false);
            paymentDetails.setValid(false);
            paymentDetailsRepository.save(paymentDetails);
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(user.getId())
                    .text(localizationLoader.getLocalizationForUser(
                        "error_payment_unknown_course", user).getData())
                    .build();
            bot.sendMessage(sendMessage);
            refund0(paymentDetails);
            return;
        }

        final CourseModel course = courseOpt.get();

        LOGGER.info("Successful payment was sent for course " + course.getName()
                + " by user " + user.getId() + ".");
        paymentDetails.setCourse(course);
        paymentDetails.setSuccessful(true);
        paymentDetails.setValid(true);
        LOGGER.info("Saving payment details...");
        addPaymentDetails(paymentDetails);
        LOGGER.info("Payment details saved.");
        SendMessage sendMessage = SendMessage.builder() // TODO: Use bot.sendMessage()
                .chatId(user.getId())
                .text(localizationLoader.getLocalizationForUser("service_successful_payment",
                    user).getData())
                .build();
        bot.sendMessage(sendMessage);
    }

    @Override
    public void refund(User user, String courseName) {
        List<PaymentDetails> paymentDetailsList = paymentDetailsRepository
                .findByUserIdAndCourseName(user.getId(), courseName);

        for (PaymentDetails paymentDetailsEntry : paymentDetailsList) {
            if (paymentDetailsEntry.isValid() && paymentDetailsEntry.isSuccessful()) {
                refund0(paymentDetailsEntry);
                return;
            }
        }
        throw new UnsupportedOperationException("Unable to refund course " + courseName
                + " for user " + user.getId()
                + " because there is no valid payment details present.");
    }

    private void refund0(PaymentDetails paymentDetails) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(paymentDetails.getUser().getId())
                .text("Course refund is currently unavailable.")
                .build();
        bot.sendMessage(sendMessage);
    }

    private boolean isAvailable0(Long userId, String courseName) {
        final CourseModel course = courseRepository.findByName(courseName).get();
        
        return !paymentDetailsRepository.findByUserIdAndCourseName(userId, course.getName())
                .isEmpty();
    }
}
