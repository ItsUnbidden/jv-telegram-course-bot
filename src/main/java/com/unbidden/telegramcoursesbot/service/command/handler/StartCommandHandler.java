package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.Blockable;
import jakarta.persistence.EntityNotFoundException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class StartCommandHandler implements CommandHandler {
    private static final Logger LOGGER = LogManager.getLogger(StartCommandHandler.class);

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    private final CourseService courseService;

    private final UserService userService;

    @Override
    @Blockable
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        final User user = message.getFrom();
        userService.updateUser(user);
        
        LOGGER.info("Sending /start message to user " + user.getId() + "...");
        final Localization localization = localizationLoader.getLocalizationForUser(
            "service_start", message.getFrom());
        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(localization.getData())
                .entities(localization.getEntities())
                .build());
        LOGGER.info("Message sent.");

        if (commandParts.length > 1) {
            LOGGER.info("Additional command parameters present: "
                    + Arrays.toString(commandParts) + ".");
            try {
                courseService.initMessage(user, commandParts[0]);
            } catch (EntityNotFoundException e) {
                LOGGER.warn("Additional parameters sent by user " + user.getId()
                        + " are invalid and will be ignored.");
            }
        }
    }

    @Override
    @NonNull
    public String getCommand() {
        return "/start";
    }

    @Override
    public boolean isAdminCommand() {
        return false;
    }
}
