package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class GetContentButtonHandler implements ButtonHandler {
    private static final String PARAM_CONTENT_ID = "${contentId}";

    private static final String SERVICE_GET_CONTENT_REQUEST = "service_get_content_request";
    private static final String SERVICE_GET_CONTENT_SUCCESS = "service_get_content_success";

    private final LocalizationLoader localizationLoader;

    private final ContentSessionService sessionService;

    private final UserService userService;

    private final ContentService contentService;

    private final TelegramBot bot;
    
    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        final UserEntity userFromDb = userService.getUser(user.getId());
        if (!userService.isAdmin(userFromDb)) {
            return;
        }
        sessionService.createSession(user, m -> {
            final String providedNumberStr = m.get(0).getText().trim();
            final Long contentId;
            try {
                contentId = Long.parseLong(m.get(0).getText().trim());
            } catch (NumberFormatException e) {
                throw new InvalidDataSentException("Unable to parse provided string "
                        + providedNumberStr + " content id long", e);
            }
            final Localization success = localizationLoader.getLocalizationForUser(
                    SERVICE_GET_CONTENT_SUCCESS, user, PARAM_CONTENT_ID,
                    contentId);
            contentService.sendContent(contentService.getById(contentId), user);
            bot.sendMessage(SendMessage.builder()
                    .chatId(user.getId())
                    .text(success.getData())
                    .entities(success.getEntities())
                    .build());
        }, true);
        final Localization request = localizationLoader.getLocalizationForUser(
                SERVICE_GET_CONTENT_REQUEST, user);

        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(request.getData())
                .entities(request.getEntities())
                .build());
    }
}
