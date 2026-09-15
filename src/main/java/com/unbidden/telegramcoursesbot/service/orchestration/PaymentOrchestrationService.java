package com.unbidden.telegramcoursesbot.service.orchestration;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.config.properties.WebhookProperties;
import com.unbidden.telegramcoursesbot.dao.ImageDao;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto.Result;
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
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseOwnership;
import com.unbidden.telegramcoursesbot.model.CurrencyCode;
import com.unbidden.telegramcoursesbot.model.ExternalInvoice;
import com.unbidden.telegramcoursesbot.model.TelegramInvoice;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.TelegramPaymentDetails;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService.PreCheckoutResponse;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

@Service
public class PaymentOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(PaymentOrchestrationService.class);

    private static final String COURSE_ID_PARAM = "courseId";

    private static final String INVOICE_IMAGES_ENDPOINT = "/invoiceimages";
    private static final String PROVIDER_TOKEN = "foo";
    public static final int MAX_PRICE = 100_000;
    
    private final ImageDao imageDao;

    private final PaymentService paymentService;

    private final ContentOrchestrationService contentService;

    private final CourseOrchestrationService courseService;

    private final MenuOrchestrationService menuService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final ReplyKeyboardRemove keyboardRemove;

    private final EntityUtil entityUtil;

    private final ValidatorUtil validatorUtil;

    private final WebhookProperties webhookProperties;

    public PaymentOrchestrationService(ImageDao imageDao, PaymentService paymentService,
            ContentOrchestrationService contentService, @Lazy CourseOrchestrationService courseService,
            MenuOrchestrationService menuService, LocalizationLoader localizationLoader, ClientManager clientManager,
            ReplyKeyboardRemove keyboardRemove, EntityUtil entityUtil, ValidatorUtil validatorUtil, WebhookProperties webhookProperties) {
        this.imageDao = imageDao;
        this.paymentService = paymentService;
        this.contentService = contentService;
        this.courseService = courseService;
        this.menuService = menuService;
        this.localizationLoader = localizationLoader;
        this.clientManager = clientManager;
        this.keyboardRemove = keyboardRemove;
        this.entityUtil = entityUtil;
        this.validatorUtil = validatorUtil;
        this.webhookProperties = webhookProperties;
    }

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

    public void sendInvoice(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = entityUtil.getCourseById(botRole, courseId);
        
        if (course.getInvoice().getClass().equals(TelegramInvoice.class)) {
            sendTelegramInvoice(botRole, course);
        } else {
            sendExternalInvoice(botRole, course);
        }
    }

    public void resolvePreCheckout(BotRole botRole, PreCheckoutQuery preCheckoutQuery) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(preCheckoutQuery, "preCheckoutQuery cannot be null");

        final AnswerPreCheckoutQueryBuilder<?, ?> answerBuilder =
                AnswerPreCheckoutQuery.builder()
                    .preCheckoutQueryId(preCheckoutQuery.getId())
                    .ok(false);
        final PreCheckoutResponse response = paymentService.checkPreCheckoutConditions(botRole, preCheckoutQuery);

        LOGGER.info("Precheckout query was sent for course " + response.course().getId()
                + " by user " + botRole.getUser().getId() + ".");

        Localization errorLoc = null;
        switch (response.result()) {
            case ALREADY_OWNED -> {
                errorLoc = localizationLoader.localize(
                        Error.PRE_CHECKOUT_COURSE_ALREADY_OWNED, botRole,
                        new Localizations.Error.PreCheckoutCourseAlreadyOwnedParams(
                            contentService.getLocalizedText(botRole, response.course().getTitle().getId())));

                LOGGER.info("Precheckout failed: user " + botRole.getUser().getId()
                        + " already has course " + response.course().getId());
            }
            case COURSE_NOT_FOUND -> {
                errorLoc = localizationLoader.localize(
                        Error.PRE_CHECKOUT_UNKNOWN_COURSE, botRole);

                LOGGER.info("Precheckout query payload contained unknown course ID: "
                        + preCheckoutQuery.getInvoicePayload() + ". User: " + botRole.getUser().getId());
            }
            case CURRENCY_MISMATCH -> {
                errorLoc = localizationLoader.localize(
                        Error.PRE_CHECKOUT_CURRENCY_MISMATCH, botRole);
                        
                LOGGER.error("Precheckout failed: currency mismatch. Investigation required. "
                        + "User: " + botRole.getUser().getId() + ", course: " + response.course().getId());
            }
            case PRICE_MISMATCH -> {
                final TelegramInvoice invoice = (TelegramInvoice)response.course().getInvoice();

                errorLoc = localizationLoader.localize(Error.PRE_CHECKOUT_PRICE_MISMATCH, botRole,
                        new Localizations.Error.PreCheckoutPriceMismatchParams(invoice.getPrice()));

                LOGGER.info("Precheckout failed: User " + botRole.getUser().getId()
                        + " used invoice with price " + preCheckoutQuery.getTotalAmount()
                        + " while course " + response.course().getId() + "'s current price is "
                        + invoice.getPrice());
            }
            default -> {
                answerBuilder.ok(true);
            }
        }

        if (errorLoc != null) {
            answerBuilder.errorMessage(errorLoc.getData());
            clientManager.sendMessage(botRole, errorLoc);
        }

        try {
            LOGGER.debug("Sending precheckout response...");
            clientManager.getClient(botRole.getBot()).execute(answerBuilder.build());
            LOGGER.info("Precheckout completed for course " + response.course().getId()
                    + " and user " + botRole.getUser().getId() + ".");
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to answer precheckout query",
                    localizationLoader.localize(Error.ANSWER_PRECHECKOUT_FAILURE, botRole), e);
        }
    }

    public void resolveSuccessfulPayment(BotRole botRole, SuccessfulPayment payment) {
        final BotRole creatorRole = entityUtil.getCreator(botRole.getBot().getId());

        LOGGER.info("Successful payment was sent for course " + payment.getInvoicePayload()
                + " by user " + botRole.getUser().getId() + ".");
        
        try {
            final TelegramPaymentDetails paymentDetails = paymentService.registerTelegramPurchase(botRole, payment);

            LOGGER.info("User " + botRole.getUser().getId() + " has bought course " + payment.getInvoicePayload()
                    + ". Payment details saved. Sending confirmation messages...");
        
            final ContentMapping courseTitleMapping = entityUtil.getMappingById(botRole, paymentDetails.getCourse().getTitle().getId());

            clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.SUCCESSFUL_PAYMENT,
                    botRole, new Localizations.Service.SuccessfulPaymentParams(
                    contentService.getLocalizedText(botRole, courseTitleMapping))));
            clientManager.sendMessage(creatorRole, localizationLoader.localize(Localizations.Service.USER_BOUGHT_COURSE,
                    creatorRole, new Localizations.Service.UserBoughtCourseParams(botRole.getUser().getFullName(),
                    contentService.getLocalizedText(creatorRole, courseTitleMapping))));
            LOGGER.debug("Messages sent.");
            if (clientManager.isOnMaintenance()) {
                throw new OnMaintenanceException("Unable to send course because server is on "
                        + "maintenance", localizationLoader.localize(
                        Error.PAYMENT_SUCCESS_SERVER_ON_MAINTENANCE, botRole));
            }
            LOGGER.debug("Initiating course " + payment.getInvoicePayload() + " for user " + botRole.getUser().getId() + "...");
            courseService.initCourse(botRole, paymentDetails.getCourse().getId());
            LOGGER.debug("Course initiated.");
        } catch (CourseIsAlreadyOwnedException e) {
            LOGGER.warn("Failed to process successfull payment due to course " + payment.getInvoicePayload()
                    + " already being owned by user " + botRole.getUser().getId() + ". Attempting refund...", e);
            
            final RefundStarPayment refundStarPayment = RefundStarPayment.builder()
                    .telegramPaymentChargeId(payment.getTelegramPaymentChargeId())
                    .userId(botRole.getUser().getId())
                    .build();
            final Course course = entityUtil.getCourseById(botRole, Long.parseLong(payment.getInvoicePayload()));
            final String courseName = contentService.getLocalizedText(botRole, course.getTitle().getId());

            try {
                LOGGER.debug("Executing automatic refund...");
                clientManager.getClient(botRole.getBot()).execute(refundStarPayment);
                clientManager.sendMessage(creatorRole, localizationLoader.localize(
                        Localizations.Service.AUTOMATIC_REFUND_NOTIFICATION, creatorRole,
                        new Localizations.Service.AutomaticRefundNotificationParams(botRole.getUser().getId(), course.getId())));
                clientManager.sendMessage(botRole, localizationLoader.localize(
                        Localizations.Service.AUTOMATIC_REFUND, botRole,
                        new Localizations.Service.AutomaticRefundParams(courseName)));
                LOGGER.info("Automatic refund has been successful and a notification has been sent to the Creator.");
            } catch (TelegramApiException e2) {
                clientManager.sendMessage(creatorRole, localizationLoader.localize(
                        Error.AUTOMATIC_REFUND_FAILURE_NOTIFICATION, creatorRole,
                            new Error.AutomaticRefundFailureNotificationParams(botRole.getUser().getId(), course.getId())));
                throw new TelegramException("Failed to automatically refund user " + botRole.getUser().getId()
                        + " after a successfull Telegram payment failed due to the course already being owned.",
                        localizationLoader.localize(Error.AUTOMATIC_REFUND_FAILURE, botRole,
                            new Error.AutomaticRefundFailureParams(courseName)), e2);
            }
        }
    }

    public CourseOwnership checkRefundPossible(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        return paymentService.checkRefundPossible(botRole, courseId);
    }

    public void refund(BotRole botRole, Long courseId, String confirmationPhrase, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(confirmationPhrase, "confirmationPhrase cannot be null");
        Assert.notEmpty(messages, "messages cannot be null");

        validatorUtil.checkExactExpectedMessages(botRole, messages, 1);  
        final String providedStr = validatorUtil.checkText(botRole, messages.getFirst());

        LOGGER.debug("User has provided this string - " + providedStr + ". Checking if this matches the confirmation phrase...");
        if (!confirmationPhrase.equals(providedStr)) {
            throw new InvalidDataSentException("Provided string does not match the confirmation phrase",
                    localizationLoader.localize(Localizations.Error.REFUND_CONFIRMATION_PHRASE_FAILURE, botRole));
        }
        LOGGER.debug("Confirmation phrase matches. Initiating refund...");

        final CourseOwnership ownership = paymentService.checkRefundPossible(botRole, courseId);
        final TelegramPaymentDetails paymentDetails = (TelegramPaymentDetails)ownership.getLastPaymentDetails();
        final RefundStarPayment refundStarPayment = RefundStarPayment.builder()
                .telegramPaymentChargeId(paymentDetails.getTelegramPaymentChargeId())
                .userId(botRole.getUser().getId())
                .build();

        try {
            clientManager.getClient(botRole.getBot()).execute(refundStarPayment);
        } catch (TelegramApiException e) {
            throw new TelegramException("Failed to send a refund request to user "
                    + botRole.getUser().getId() + ".", localizationLoader
                    .localize(Error.REFUND_FAILURE, botRole), e);
        }
        LOGGER.debug("Invalidating ownership for course " + courseId
                + " and user " + botRole.getUser().getId() + "..."); 
        paymentService.invalidateTelegramCourseOwnership(botRole, courseId);
        LOGGER.info("Ownership " + ownership.getId() + " has been invalidated.");
        LOGGER.debug("Sending confirmation messages...");
        final String courseName = contentService.getLocalizedText(botRole, entityUtil.getCourseById(
                botRole, courseId).getTitle().getId());
        final BotRole creatorRole = entityUtil.getCreator(botRole.getBot().getId());

        clientManager.sendMessage(botRole, localizationLoader
                .localize(Localizations.Service.REFUND_SUCCESS, botRole,
                    new Localizations.Service.RefundSuccessParams(courseName)));
        clientManager.sendMessage(creatorRole, localizationLoader.localize(Localizations.Service.USER_REFUNDED_COURSE,
                creatorRole, new Localizations.Service.UserRefundedCourseParams(courseName, botRole.getUser().getFullName())));
        LOGGER.debug("Messages sent.");
    }

    public void giftCourse(BotRole botRole, Long targetId, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        LOGGER.info("User " + botRole.getUser().getId() + " is trying to gift course " + courseId + " to user " + targetId + "...");
        final CourseOwnership ownership = paymentService.registerGiftCourse(botRole, targetId, courseId);
        
        LOGGER.info("Course ownership " + ownership.getId() + " has been created/updated for user "
                + botRole.getUser().getId() + " and course " + courseId + ".");
        final BotRole targetBotRole = entityUtil.getActiveBotRole(botRole, targetId);
        final ContentMapping courseTitleMapping = entityUtil.getMappingById(botRole, ownership.getCourse().getTitle().getId());

        LOGGER.debug("Sending confirmation messages...");
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.COURSE_GIFTED_SUCCESSFULLY,
                botRole, new Localizations.Service.CourseGiftedSuccessfullyParams(
                contentService.getLocalizedText(botRole, courseTitleMapping), ownership.getUser().getFullName(),
                entityUtil.getLocalizedTitle(botRole, targetBotRole))));
        clientManager.sendMessage(targetBotRole, localizationLoader.localize(Localizations.Service.COURSE_GIFTED_NOTIFICATION,
                targetBotRole, new Localizations.Service.CourseGiftedNotificationParams(
                contentService.getLocalizedText(targetBotRole, courseTitleMapping),
                entityUtil.getLocalizedTitle(targetBotRole, botRole), botRole.getUser().getFullName())));
        LOGGER.debug("Messages sent.");
    }

    public void takeCourse(BotRole botRole, Long targetId, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        LOGGER.info("User " + botRole.getUser().getId() + " is trying to take away course " + courseId + " from user " + targetId + "...");
        final CourseOwnership ownership = paymentService.invalidateGiftCourse(botRole, targetId, courseId);

        LOGGER.info("Course ownership " + ownership.getId() + " has been invalidated for user "
                + botRole.getUser().getId() + " and course " + courseId + ".");
        final BotRole targetBotRole = entityUtil.getActiveBotRole(botRole, targetId);
        final ContentMapping courseTitleMapping = entityUtil.getMappingById(botRole, ownership.getCourse().getTitle().getId());

        LOGGER.debug("Sending confirmation messages...");
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.COURSE_TAKEN_SUCCESSFULLY,
                botRole, new Localizations.Service.CourseTakenSuccessfullyParams(
                contentService.getLocalizedText(botRole, courseTitleMapping), ownership.getUser().getFullName(),
                entityUtil.getLocalizedTitle(botRole, targetBotRole))), keyboardRemove);
        clientManager.sendMessage(targetBotRole, localizationLoader.localize(Localizations.Service.COURSE_TAKEN_NOTIFICATION,
                targetBotRole, new Localizations.Service.CourseTakenNotificationParams(
                contentService.getLocalizedText(targetBotRole, courseTitleMapping), botRole.getUser().getFullName(),
                entityUtil.getLocalizedTitle(targetBotRole, botRole))));
        LOGGER.debug("Messages sent.");
    }

    public void deleteInvoiceImage(BotRole botRole, List<Message> messages) {
        validatorUtil.checkExactExpectedMessages(botRole, messages, 1);

        final Long courseId = validatorUtil.parseId(botRole, messages.getFirst());

        if (!imageDao.exists(courseId)) {
            throw new InvalidDataSentException("Invoice image does not exist for course "
                    + courseId, localizationLoader.localize(Localizations.Error.INVOICE_IMAGE_DOES_NOT_EXIST, botRole));
        }

        LOGGER.info("Deleing invoice image for course " + courseId + "...");
        imageDao.delete(courseId);
        LOGGER.info("Image deleted.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader
                .localize(Localizations.Service.INVOICE_IMAGE_DELETED, botRole));
        LOGGER.debug("Message sent.");
    }

    private void sendTelegramInvoice(BotRole botRole, Course course) {
        final String imageUrl = webhookProperties.url() + INVOICE_IMAGES_ENDPOINT + "/" + course.getId();
        final String courseName = contentService.getLocalizedText(botRole, course.getTitle().getId());
        final TelegramInvoice invoice = (TelegramInvoice)course.getInvoice();

        LOGGER.debug("Compiling invoice for course " + course.getId() + " for user " + botRole.getUser().getId() + "...");
        final SendInvoiceBuilder<?, ?> builder = SendInvoice.builder()
                .chatId(botRole.getUser().getId())
                .title(courseName)
                .description(contentService.getLocalizedText(botRole, invoice.getDescription().getId()))
                .payload(course.getId().toString())
                .providerToken(PROVIDER_TOKEN)
                .currency(CurrencyCode.XTR.toString())
                .price(LabeledPrice.builder()
                    .amount(invoice.getPrice())
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
                    + " to user " + botRole.getUser().getId() + "...");
            clientManager.getClient(botRole.getBot()).execute(builder.build());
            LOGGER.debug("Invoice sent.");
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send invoice for "
                    + course.getId() + " to user " + botRole.getUser().getId(), localizationLoader
                    .localize(Error.SEND_INVOICE_FAILURE, botRole), e);
        }
    }

    private void sendExternalInvoice(BotRole botRole, Course course) {
        LOGGER.debug("Sending an external invoice for course " + course.getId() + "...");

        final ExternalInvoice invoice = (ExternalInvoice)course.getInvoice();
        final List<SendMessageResultDto> sentMessages = contentService.sendLocalizedContent(
                botRole, invoice.getMapping().getId());
        final Optional<SendMessageResultDto> failureOpt = sentMessages.stream().filter(dto -> dto.getResult() != Result.OK).findAny();

        if (failureOpt.isPresent()) {
            throw new TelegramException("Failed to send one or more of the external invoice messages for course "
                    + course.getId() + " to user " + botRole.getUser().getId() + ".",
                    localizationLoader.localize(Localizations.Error.SEND_EXTERNAL_INVOICE, botRole),
                    failureOpt.get().getException());
        }
        final SendMessageResultDto sentMessageDto;

        if (sentMessages.size() > 1) {
            LOGGER.debug("The content for course " + course.getId() + "'s external invoice is a media group.");

            sentMessageDto = clientManager.sendMessage(botRole, localizationLoader
                    .localize(Localizations.Service.COURSE_EXTERNAL_INVOICE_MEDIA_GROUP_BYPASS, botRole));
            LOGGER.debug("Additional message for the menu has been sent.");
        } else {
            LOGGER.debug("The content for course " + course.getId() + "'s invoice is not a media group. "
                    + "The menu will be attached to the message.");    
            sentMessageDto = sentMessages.getFirst();
        }

        menuService.initiateMenu(botRole, MenuKey.EXTERNAL_INVOICE, COURSE_ID_PARAM,
                course.getId().toString(), sentMessageDto.getMessage().getMessageId());
        LOGGER.debug("External invoice menu has been sent.");
    }
}
