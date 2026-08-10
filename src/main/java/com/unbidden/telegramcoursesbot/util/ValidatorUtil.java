package com.unbidden.telegramcoursesbot.util;

import java.util.List;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ValidatorUtil {
    private final LocalizationLoader localizationLoader;

    public void checkExpectedMessages(UserEntity user, List<Message> messages, int number) {
        if (messages.size() != number) {
            throw new InvalidDataSentException("There are supposed to be "
                    + number + " messages. User " + user.getId()
                    + " has sent " + messages.size() + " messages though.",
                    localizationLoader.getLocalizationForUser(
                    Error.NUMBER_OF_MESSAGES, user, new Error.NumberOfMessagesParams(messages.size(), number)));
        }
        for (int i = 0; i < messages.size(); i++) {
            if (!messages.get(i).hasText()) {
                throw new InvalidDataSentException("Message " + messages.get(i)
                        .getMessageId() + " sent by user " + user.getId()
                        + " does not have any text.",
                        localizationLoader.getLocalizationForUser(
                        Error.MESSAGE_TEXT_MISSING, user, new Error.MessageTextMissingParams(i + 1)));
            }
        }
    }
}
