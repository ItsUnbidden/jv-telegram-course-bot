package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.PaymentDetails;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(RefundButtonHandler.class);

    private static final String PARAM_PROVIDED_MESSAGES_AMOUNT = "${providedMessagesNumber}";
    private static final String PARAM_EXPECTED_MESSAGES_AMOUNT = "${expectedMessagesAmount}";
    private static final String PARAM_CONFIRMATION_PHRASE = "${confirmationPhrase}";
    private static final String PARAM_SPENT_STARS = "${spentStars}";
    private static final String PARAM_COURSE_NAME = "${courseName}";

    private static final String SERVICE_REFUND_CONFIRMATION_REQUEST =
            "service_refund_confirmation_request";
    private static final String SERVICE_REFUND_CONFIRMATION_PHRASE =
            "service_refund_confirmation_phrase";

    private static final String COURSE_NAME = "course_%s_name";
        
    private static final String ERROR_AMOUNT_OF_MESSAGES = "error_amount_of_messages";
    private static final String ERROR_TEXT_MESSAGE_EXPECTED = "error_text_message_expected";
    private static final String ERROR_REFUND_CONFIRMATION_PHRASE_FAILURE =
            "error_refund_confirmation_phrase_failure";

    private final PaymentService paymentService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.REFUND)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        LOGGER.info("User " + user.getId() + " is trying to refund course " + params[0] + "...");
        final PaymentDetails paymentDetails = paymentService
                .isRefundPossible(user, bot, params[0]);
        final Map<String, Object> confirmationPhraseParameterMap = new HashMap<>();
        confirmationPhraseParameterMap.put(PARAM_COURSE_NAME, localizationLoader
                .getLocalizationForUser(COURSE_NAME.formatted(params[0]), user).getData());
        confirmationPhraseParameterMap.put(PARAM_SPENT_STARS, paymentDetails.getTotalAmount());

        final String confirmationPhrase = localizationLoader.getLocalizationForUser(
                SERVICE_REFUND_CONFIRMATION_PHRASE, user,
                confirmationPhraseParameterMap).getData();

        sessionService.createSession(user, bot, m -> {
            if (m.size() != 1) {
                final Map<String, Object> parameterMap = new HashMap<>();
                    parameterMap.put(PARAM_EXPECTED_MESSAGES_AMOUNT, 1);
                    parameterMap.put(PARAM_PROVIDED_MESSAGES_AMOUNT, m.size());

                    throw new InvalidDataSentException("One message was expected but "
                            + m.size() + " was/were sent", localizationLoader
                            .getLocalizationForUser(ERROR_AMOUNT_OF_MESSAGES, user,
                            parameterMap));
            }
            if (!m.get(0).hasText()) {
                throw new InvalidDataSentException("A text message was expected",
                localizationLoader.getLocalizationForUser(ERROR_TEXT_MESSAGE_EXPECTED, user));
            }
            final String providedStr = m.get(0).getText();
            LOGGER.debug("User has provided this string - " + providedStr
                    + ". Checking if this matches the confirmation phrase...");
            if (!confirmationPhrase.equals(providedStr.trim())) {
                throw new InvalidDataSentException("Provided string does not match "
                        + "the confirmation phrase", localizationLoader.getLocalizationForUser(
                        ERROR_REFUND_CONFIRMATION_PHRASE_FAILURE, user));
            }
            LOGGER.debug("Confirmation aquired. Initiating refund...");
            paymentService.refund(user, bot, params[0]);
            LOGGER.info("Course " + params[0] + " has been refunded for user "
                    + user.getId() + ".");
        });
        LOGGER.debug("Sending request message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.getLocalizationForUser(
                SERVICE_REFUND_CONFIRMATION_REQUEST, user,
                PARAM_CONFIRMATION_PHRASE, confirmationPhrase));
        LOGGER.debug("Message sent.");
    }
}
