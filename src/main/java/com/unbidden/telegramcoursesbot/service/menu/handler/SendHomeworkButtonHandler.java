package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendHomeworkButtonHandler implements ButtonHandler {
    private static final String SERVICE_SEND_HOMEWORK_REQUEST = "service_send_homework_request";

    private final HomeworkService homeworkService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        final Long homeworkProgressId = Long.parseLong(params[0]);

        sessionService.createSession(user, m ->
                homeworkService.commit(homeworkProgressId, m));

        final Localization localization = localizationLoader.getLocalizationForUser(
                SERVICE_SEND_HOMEWORK_REQUEST, user);
        
        client.sendMessage(user, localization);
    }
}
