package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.Content;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.SessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class UploadContentButtonHandler implements ButtonHandler {
    private final LocalizationLoader localizationLoader;

    private final SessionService sessionService;

    private final UserService userService;

    private final TelegramBot bot;
    
    @Override
    public void handle(String[] params, User user) {
        if (!userService.isAdmin(user)) {
            return;
        }
        sessionService.createSession(user, m -> {
            final Content content = bot.parseAndPersistContent(m);
            final Localization success = localizationLoader.getLocalizationForUser(
                    "service_upload_content_success", m.getFrom(), "${contentId}",
                    content.getId());
            bot.sendMessage(SendMessage.builder()
                    .chatId(m.getFrom().getId())
                    .text(success.getData())
                    .entities(success.getEntities())
                    .build());
        }, false);
        final Localization request = localizationLoader.getLocalizationForUser(
                "service_upload_content_request", user);

        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(request.getData())
                .entities(request.getEntities())
                .build());
    }
}
