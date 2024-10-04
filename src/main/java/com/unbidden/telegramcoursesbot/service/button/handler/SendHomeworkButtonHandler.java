package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class SendHomeworkButtonHandler implements ButtonHandler {
    private final HomeworkService homeworkService;

    private final SessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(String[] params, User user) {
        final Long homeworkProgressId = Long.parseLong(params[0]);

        sessionService.createSession(user, m -> {
            homeworkService.process(homeworkProgressId, m);
        }, false);

        final Localization localization = localizationLoader.getLocalizationForUser(
                "service_send_homework_request", user);
        
        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(localization.getData())
                .entities(localization.getEntities())
                .build());
    }
}
