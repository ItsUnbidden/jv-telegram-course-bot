package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class SendHomeworkButtonHandler implements ButtonHandler {
    private static final String SERVICE_SEND_HOMEWORK_REQUEST = "service_send_homework_request";

    private final HomeworkService homeworkService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        final Long homeworkProgressId = Long.parseLong(params[0]);

        sessionService.createSession(user, m ->
                homeworkService.commit(homeworkProgressId, m));

        final Localization localization = localizationLoader.getLocalizationForUser(
                SERVICE_SEND_HOMEWORK_REQUEST, user);
        
        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(localization.getData())
                .entities(localization.getEntities())
                .build());
    }
}
