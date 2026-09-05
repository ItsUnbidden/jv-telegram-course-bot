package com.unbidden.telegramcoursesbot.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.config.properties.LocalizationsProperties;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.content.Document;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ValidatorUtil {
    private static final String MENU = "menu";
    private static final String ERROR = "error";
    private static final String BUTTON = "button";
    private static final String SERVICE = "service";

    private final LocalizationLoader loader;

    private final LocalizationsProperties localizationsProperties;

    public void checkExactExpectedMessages(BotRole botRole, List<Message> messages, int number) {
        if (messages.size() != number) {
            throw new InvalidDataSentException("There are supposed to be "
                    + number + " messages. User " + botRole.getUser().getId()
                    + " has sent " + messages.size() + " messages though.",
                    loader.localize(Localizations.Error.NUMBER_OF_MESSAGES, botRole,
                        new Localizations.Error.NumberOfMessagesParams(messages.size(), number)));
        }
    }

    public void checkAtLeastExpectedMessages(BotRole botRole, List<Message> messages, int number) {
        if (messages.size() < number) {
            throw new InvalidDataSentException("There are supposed to be at least "
                    + number + " messages. User " + botRole.getUser().getId()
                    + " has sent " + messages.size() + " messages though.",
                    loader.localize(Localizations.Error.NUMBER_OF_MESSAGES, botRole,
                        new Localizations.Error.AtLeastNumberOfMessagesParams(messages.size(), number)));
        }
    }

    /**
     * Checks whether the message contains a language code. If the text cannot be considered a language code,
     * an {@link InvalidDataSentException} is thrown. If there is no text, returns {@code false}.
     * @param user
     * @param message
     * @return false if the code was not found, true if success
     */
    public boolean checkLanguageCode(BotRole botRole, Message message) {
        if (message.hasText()) {
            final String text = message.getText().trim();

            if (text.length() > 3 || text.length() < 2) {
                throw new InvalidDataSentException("Language code must be "
                        + "between 2 and 3 characters long.", loader.localize(
                            Localizations.Error.LANGUAGE_CODE_LENGTH, botRole));
            }
            return true;
        }
        return false;
    }

    public Long parseId(BotRole botRole, Message message) {
        try {
            final Long id = Long.parseLong(message.getText().trim());

            if (id < 1) {
                throw new InvalidDataSentException("An ID cannot be less than 1.", loader.localize(
                        Localizations.Error.PARSE_ID_BOUNDS_FAILURE, botRole));
            }
            return id;
        } catch (NumberFormatException e) {
            throw new InvalidDataSentException("Unable to parse string " + message.getText()
                    + " to an id", loader.localize(Localizations.Error.PARSE_ID_FAILURE, botRole));
        }
    }

    public Integer parseInt(BotRole botRole, Message message) {
        try {
            return Integer.parseInt(message.getText().trim());
        } catch (NumberFormatException e) {
            throw new InvalidDataSentException("Unable to parse string " + message.getText()
                    + " to an int.", loader.localize(Localizations.Error.PARSE_INT_FAILURE, botRole));
        }
    }

    /**
     * Parses the message into an {@link Integer}. It will enforce a lower and an upper bounds (both inclusive).
     * If the message cannot be parsed or the bounds are incorrect, throws an {@link InvalidDataSentException}.
     * @param user
     * @param message
     * @param lowerBound
     * @param upperBound
     * @return the integer
     */
    public Integer parseIntInBounds(BotRole botRole, Message message, int lowerBound, int upperBound) {
        final Integer number = parseInt(botRole, message);

        if (number < lowerBound || number > upperBound) {
            throw new InvalidDataSentException("The number must be between " + lowerBound + " and "
                    + upperBound + " (inclusive).", loader.localize(Localizations.Error.PARSE_INT_BOUNDS_FAILURE, botRole,
                        new Localizations.Error.ParseIntBoundsFailureParams(lowerBound, upperBound)));
        }
        return number;
    }

    /**
     * Checks whether the message has text of the required length. If there is no text, returns false. If the text has incorrect length, throws an {@link InvalidDataSentException}.
     * @param user
     * @param message
     * @param lowerBound
     * @param upperBound
     * @return false if the message has no text, true if success
     */
    public boolean checkTextLength(BotRole botRole, Message message, int lowerBound, int upperBound) {
        if (message.hasText()) {
            if (message.getText().length() < lowerBound || message.getText().length() > upperBound) {
                throw new InvalidDataSentException("The message must contain text that has a length between " + lowerBound 
                        + " and " + upperBound + " characters.", loader.localize(Localizations.Error.TEXT_BOUNDS_FAILURE, botRole,
                        new Localizations.Error.TextBoundsFailureParams(lowerBound, upperBound)));
            }

            return true;
        }
        return false;
    }

    /**
     * Checks whether the message has any meaningful text. Blank strings are not considered meaningful.
     * @param user
     * @param message
     * @return trimmed string from the message
     */
    public String checkText(BotRole botRole, Message message) {
        if (!message.hasText() || message.getText().isBlank()) {
            throw new InvalidDataSentException("A meaningful text message was expected.",
                    loader.localize(Localizations.Error.TEXT_MESSAGE_EXPECTED, botRole));
        }
        return message.getText().trim();
    }

    public void checkIfDocumentIsALocalization(BotRole botRole, Document document) {
        final String fileName = document.getFileName();
        final List<String> possibleNames = new ArrayList<>();
        
        possibleNames.add(SERVICE + localizationsProperties.format());
        possibleNames.add(BUTTON + localizationsProperties.format());
        possibleNames.add(ERROR + localizationsProperties.format());
        possibleNames.add(MENU + localizationsProperties.format());

        if (!possibleNames.contains(fileName)) {
            throw new InvalidDataSentException("File " + fileName + " cannot be used for "
                    + "localizations since it has an unknown name. Available names: "
                    + possibleNames + ".", loader.localize(Localizations.Error.FILE_NOT_LOCALIZATION, botRole));
        }
    }

    public URI checkUri(BotRole botRole, Message message) {
        final String trimmed = checkText(botRole, message);

        try {
            final URI uri = URI.create(trimmed);

            if (uri.getScheme() == null || !uri.getScheme().equals("https")) {
                throw new InvalidDataSentException("URL does not contain the correct scheme. Scheme: " + uri.getScheme() + ".",
                        loader.localize(Localizations.Error.PARSE_URL_FAILURE_INVALID_SCHEME, botRole));
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidDataSentException("URL does not contain a host.",
                        loader.localize(Localizations.Error.PARSE_URL_FAILURE, botRole));
            }
            
            return uri;
        } catch (IllegalArgumentException e) {
            throw new InvalidDataSentException("Unable to parse string to a URL. String: " + trimmed + ".",
                    loader.localize(Localizations.Error.PARSE_URL_FAILURE, botRole), e);
        }
    }
}
