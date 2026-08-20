package com.unbidden.telegramcoursesbot.service.orchestration;

import java.util.List;

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
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.exception.OnMaintenanceException;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseOwnership;
import com.unbidden.telegramcoursesbot.model.CurrencyCode;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.Course.PaymentType;
import com.unbidden.telegramcoursesbot.model.TelegramPaymentDetails;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService.PreCheckoutResponse;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(PaymentOrchestrationService.class);

    private static final String COURSE_ID_PARAM = "courseId";

    private static final String INVOICE_IMAGES_ENDPOINT = "/invoiceimages";
    private static final String PROVIDER_TOKEN = "foo";
    
    private final ImageDao imageDao;

    private final PaymentService paymentService;

    private final ContentOrchestrationService contentService;

    private final CourseOrchestrationService courseService;

    private final MenuOrchestrationService menuService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    private final ValidatorUtil validatorUtil;

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
                errorLoc = localizationLoader.localize(
                        Error.PRE_CHECKOUT_COURSE_ALREADY_OWNED, user,
                        new Localizations.Error.PreCheckoutCourseAlreadyOwnedParams(
                            contentService.getLocalizedText(user, bot, response.course().getTitle().getId())));

                LOGGER.info("Precheckout failed: user " + user.getId()
                        + " already has course " + response.course().getId());
            }
            case COURSE_NOT_FOUND -> {
                errorLoc = localizationLoader.localize(
                        Error.PRE_CHECKOUT_UNKNOWN_COURSE, user);

                LOGGER.info("Precheckout query payload contained unknown course ID: "
                        + preCheckoutQuery.getInvoicePayload() + ". User: " + user.getId());
            }
            case CURRENCY_MISMATCH -> {
                errorLoc = localizationLoader.localize(
                        Error.PRE_CHECKOUT_CURRENCY_MISMATCH, user);
                        
                LOGGER.error("Precheckout failed: currency mismatch. Investigation required. "
                        + "User: " + user.getId() + ", course: " + response.course().getId());
            }
            case PRICE_MISMATCH -> {
                errorLoc = localizationLoader.localize(
                        Error.PRE_CHECKOUT_PRICE_MISMATCH, user,
                        new Localizations.Error.PreCheckoutPriceMismatchParams(response.course().getPrice()));

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
                    localizationLoader.localize(Error.ANSWER_PRECHECKOUT_FAILURE, user), e);
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
        
            final ContentMapping courseTitleMapping = entityUtil.getMappingById(user, bot, paymentDetails.getCourse().getTitle().getId());

            clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                    Localizations.Service.SUCCESSFUL_PAYMENT, user, new Localizations.Service.SuccessfulPaymentParams(
                        contentService.getLocalizedText(user, bot, courseTitleMapping))));
            clientManager.getClient(bot).sendMessage(creator, localizationLoader.localize(
                    Localizations.Service.USER_BOUGHT_COURSE, creator, new Localizations.Service.UserBoughtCourseParams(user.getFullName(),
                    contentService.getLocalizedText(creator, bot, courseTitleMapping))));
            LOGGER.debug("Messages sent.");
            if (clientManager.isOnMaintenance()) {
                throw new OnMaintenanceException("Unable to send course because server is on "
                        + "maintenance", localizationLoader.localize(
                        Error.PAYMENT_SUCCESS_SERVER_ON_MAINTENANCE, user));
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
            final Course course = entityUtil.getCourseById(user, bot, Long.parseLong(payment.getInvoicePayload()));
            final String courseName = contentService.getLocalizedText(user, bot, course.getTitle().getId());

            try {
                LOGGER.debug("Executing automatic refund...");
                clientManager.getClient(bot).execute(refundStarPayment);
                clientManager.getClient(bot).sendMessage(creator, localizationLoader.localize(
                        Localizations.Service.AUTOMATIC_REFUND_NOTIFICATION, creator,
                        new Localizations.Service.AutomaticRefundNotificationParams(user.getId(), course.getId())));
                clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                        Localizations.Service.AUTOMATIC_REFUND, user,
                        new Localizations.Service.AutomaticRefundParams(courseName)));
                LOGGER.info("Automatic refund has been successful and a notification has been sent to the Creator.");
            } catch (TelegramApiException e2) {
                clientManager.getClient(bot).sendMessage(creator, localizationLoader.localize(
                        Error.AUTOMATIC_REFUND_FAILURE_NOTIFICATION, creator,
                            new Error.AutomaticRefundFailureNotificationParams(user.getId(), course.getId())));
                throw new TelegramException("Failed to automatically refund user " + user.getId()
                        + " after a successfull Telegram payment failed due to the course already being owned.",
                        localizationLoader.localize(Error.AUTOMATIC_REFUND_FAILURE, user,
                            new Error.AutomaticRefundFailureParams(courseName)), e2);
            }
        }
    }

    public CourseOwnership checkRefundPossible(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        return paymentService.checkRefundPossible(user, bot, courseId);
    }

    public void refund(UserEntity user, Bot bot, Long courseId, String confirmationPhrase, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(confirmationPhrase, "confirmationPhrase cannot be null");
        Assert.notEmpty(messages, "messages cannot be null");

        validatorUtil.checkExactExpectedMessages(user, messages, 1);  
        final String providedStr = validatorUtil.checkText(user, messages.getFirst());

        LOGGER.debug("User has provided this string - " + providedStr + ". Checking if this matches the confirmation phrase...");
        if (!confirmationPhrase.equals(providedStr)) {
            throw new InvalidDataSentException("Provided string does not match the confirmation phrase",
                    localizationLoader.localize(Localizations.Error.REFUND_CONFIRMATION_PHRASE_FAILURE, user));
        }
        LOGGER.debug("Confirmation phrase matches. Initiating refund...");

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
                    .localize(Error.REFUND_FAILURE, user), e);
        }
        LOGGER.debug("Invalidating ownership for course " + courseId
                + " and user " + user.getId() + "..."); 
        paymentService.invalidateTelegramCourseOwnership(user, bot, courseId);
        LOGGER.info("Ownership " + ownership.getId() + " has been invalidated.");
        LOGGER.debug("Sending confirmation messages...");
        final String courseName = contentService.getLocalizedText(user, bot, entityUtil.getCourseById(
                user, bot, courseId).getTitle().getId());

        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .localize(Localizations.Service.REFUND_SUCCESS, user,
                    new Localizations.Service.RefundSuccessParams(courseName)));
        clientManager.getClient(bot).sendMessage(entityUtil.getCreator(bot),
                localizationLoader.localize(Localizations.Service.USER_REFUNDED_COURSE,
                user, new Localizations.Service.UserRefundedCourseParams(courseName, user.getFullName())));
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
        final ContentMapping courseTitleMapping = entityUtil.getMappingById(user, bot, ownership.getCourse().getTitle().getId());

        LOGGER.debug("Sending confirmation messages...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.COURSE_GIFTED_SUCCESSFULLY, user, new Localizations.Service.CourseGiftedSuccessfullyParams(
                    contentService.getLocalizedText(user, bot, courseTitleMapping), ownership.getUser().getFullName(),
                    entityUtil.getLocalizedTitle(user, bot, ownership.getUser()))));
        clientManager.getClient(bot).sendMessage(ownership.getUser(), localizationLoader.localize(
                Localizations.Service.COURSE_GIFTED_NOTIFICATION, ownership.getUser(), new Localizations.Service.CourseGiftedNotificationParams(
                    contentService.getLocalizedText(ownership.getUser(), bot, courseTitleMapping), user.getFullName(),
                    entityUtil.getLocalizedTitle(ownership.getUser(), bot, user))));
        LOGGER.debug("Messages sent.");
    }

    public void takeCourse(UserEntity user, Bot bot, Long targetId, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        LOGGER.info("User " + user.getId() + " is trying to take away course " + courseId + " from user " + targetId + "...");
        final CourseOwnership ownership = paymentService.invalidateGiftCourse(user, bot, targetId, courseId);
        LOGGER.info("Course ownership " + ownership.getId() + " has been invalidated for user "
                + user.getId() + " and course " + courseId + ".");
        final ContentMapping courseTitleMapping = entityUtil.getMappingById(user, bot, ownership.getCourse().getTitle().getId());

        LOGGER.debug("Sending confirmation messages...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.COURSE_TAKEN_SUCCESSFULY, user, new Localizations.Service.CourseTakenSuccessfullyParams(
                    contentService.getLocalizedText(user, bot, courseTitleMapping), ownership.getUser().getFullName(),
                    entityUtil.getLocalizedTitle(user, bot, ownership.getUser()))));
        clientManager.getClient(bot).sendMessage(ownership.getUser(), localizationLoader.localize(
                Localizations.Service.COURSE_TAKEN_NOTIFICATION, ownership.getUser(), new Localizations.Service.CourseTakenNotificationParams(
                    contentService.getLocalizedText(ownership.getUser(), bot, courseTitleMapping), user.getFullName(),
                    entityUtil.getLocalizedTitle(ownership.getUser(), bot, user))));
        LOGGER.debug("Messages sent.");
    }

    public void deleteInvoiceImage(UserEntity user, Bot bot, List<Message> messages) {
        validatorUtil.checkExactExpectedMessages(user, messages, 1);

        final Long courseId = validatorUtil.parseId(user, messages.getFirst());

        if (!imageDao.exists(courseId)) {
            throw new InvalidDataSentException("Invoice image does not exist for course "
                    + courseId, localizationLoader.localize(
                    Localizations.Error.INVOICE_IMAGE_DOES_NOT_EXIST, user));
        }

        LOGGER.info("Deleing invoice image for course " + courseId + "...");
        imageDao.delete(courseId);
        LOGGER.info("Image deleted.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.getBotLordClient().sendMessage(user, localizationLoader
                .localize(Localizations.Service.INVOICE_IMAGE_DELETED, user));
        LOGGER.debug("Message sent.");
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
                    : "") // TODO: potentially remove the not-null check if descriptions become mandatory
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
                    .localize(Error.SEND_INVOICE_FAILURE, user), e);
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
                    .localize(Localizations.Service.COURSE_EXTERNAL_INVOICE_MEDIA_GROUP_BYPASS, user));
            LOGGER.debug("Additional message for the menu has been sent.");
        } else {
            LOGGER.debug("The content for course " + course.getId() + "'s invoice is not a media group. "
                    + "The menu will be attached to the message.");    
            menuMessage = sentMessages.getFirst();
        }

        menuService.initiateMenu(user, bot, MenuKey.EXTERNAL_INVOICE, COURSE_ID_PARAM,
                course.getId().toString(), menuMessage.getMessageId());
        LOGGER.debug("External invoice menu has been sent.");
    }
}
