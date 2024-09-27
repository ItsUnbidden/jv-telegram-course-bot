package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.UserRepository;
import com.unbidden.telegramcoursesbot.service.course.CourseFlow;
import com.unbidden.telegramcoursesbot.service.course.CourseServiceSupplier;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.Blockable;
import java.util.Arrays;
import java.util.Optional;
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

    private final UserRepository userRepository;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    private final CourseServiceSupplier courseServiceSupplier;

    private final UserService userService;

    @Override
    @Blockable
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        final User user =
                message.getFrom();
        final UserEntity mappedUser = new UserEntity(user);
        
        Optional<UserEntity> userFromDbOpt = userRepository.findById(user.getId());
        
        if (userFromDbOpt.isEmpty() || !userFromDbOpt.get().equals(mappedUser)) {
            if (user.getId().longValue() == userService.getDefaultAdminId().longValue()) {
                LOGGER.info("User " + user.getId() + " is the default admin.");
                mappedUser.setAdmin(true);
            }
            LOGGER.info("User " + user.getId()
                    + " is new or their profile has changed. Saving...");
            userRepository.save(mappedUser);
            LOGGER.info("User has been saved to DB.");
        }
        
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
            CourseFlow service = courseServiceSupplier.getService(commandParts[1]);
            if (service != null) {
                LOGGER.info("Initial message for course " + commandParts[1]
                        + " will be send to user " + user.getId());
                service.initMessage(user);
                return;
            }
            LOGGER.warn("Additional parameters sent by user " + user.getId()
                    + " are invalid and will be ignored.");
        }
    }

    @Override
    @NonNull
    public String getCommand() {
        return "/start";
    }
}
