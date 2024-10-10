package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.Content;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.SessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class UploadContentButtonHandler implements ButtonHandler {
    private static final String SERVICE_UPLOAD_CONTENT_REQUEST = "service_upload_content_request";
    private static final String SERVICE_UPLOAD_CONTENT_SUCCESS = "service_upload_content_success";

    private final LocalizationLoader localizationLoader;

    private final SessionService sessionService;

    private final UserService userService;

    private final TelegramBot bot;
    
    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (!userService.isAdmin(user)) {
            return;
        }
        sessionService.createSession(user, false, m -> {
            final Content content = bot.parseAndPersistContent(m);
            final Localization success = localizationLoader.getLocalizationForUser(
                    SERVICE_UPLOAD_CONTENT_SUCCESS, m.getFrom(), "${contentId}",
                    content.getId());
            bot.sendMessage(SendMessage.builder()
                    .chatId(m.getFrom().getId())
                    .text(success.getData())
                    .entities(success.getEntities())
                    .build());
        });
        final Localization request = localizationLoader.getLocalizationForUser(
                SERVICE_UPLOAD_CONTENT_REQUEST, user);

        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(request.getData())
                .entities(request.getEntities())
                .build());
    }
}
