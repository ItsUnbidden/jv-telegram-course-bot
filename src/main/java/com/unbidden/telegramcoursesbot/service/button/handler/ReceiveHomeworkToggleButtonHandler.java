package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class ReceiveHomeworkToggleButtonHandler implements ButtonHandler {
    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void handle(String[] params, User user) {
        final UserEntity updatedUser = userService.toogleReceiveHomework(
                userService.getUser(user.getId()));
        final Localization success = localizationLoader.getLocalizationForUser(
                "service_toggle_receive_homework", user, "${status}",
                (updatedUser.isReceivingHomeworkRequests()) ? "ENABLED" : "DISABLED");

        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(success.getData())
                .entities(success.getEntities())
                .build());
    }
}
