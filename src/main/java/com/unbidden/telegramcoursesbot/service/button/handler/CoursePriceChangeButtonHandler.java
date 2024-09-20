package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.dao.LocalizationLoader;
import com.unbidden.telegramcoursesbot.model.CourseModel;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.service.session.SessionService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class CoursePriceChangeButtonHandler implements ButtonHandler {
    private static final Logger LOGGER =
            LogManager.getLogger(CoursePriceChangeButtonHandler.class);

    private final TelegramBot bot;

    private final CourseRepository courseRepository;

    private final SessionService sessionService;

    private final LocalizationLoader localizationLoader;

    @Override
    public void handle(String[] params, User user) {
        final CourseModel course = courseRepository.findByName(params[0]).get();
        LOGGER.info("Course price change handler was triggered. Current value is: "
                + course.getPrice() + ".");
        sessionService.createSession(user, (m) -> {
            String response = localizationLoader.getTextByNameForUser(
                "error_new_price_cannot_parse", user);
            if (m.hasText()) {
                try {
                    int newPrice = Integer.parseInt(m.getText().trim());
                    LOGGER.info("New price " + newPrice + " for course " + course.getName()
                            + " parsed successfuly.");
                    course.setPrice(newPrice);
                    courseRepository.save(course);
                    LOGGER.info("New price saved.");
                    response = localizationLoader.getTextByNameForUser(
                            "message_course_price_update_success", user);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Unable to parse new price provided by the user.");
                }
            } else {
                LOGGER.warn("Message provided by the user does not have any text.");
            }
            bot.sendMessage(SendMessage.builder()
                        .chatId(user.getId())
                        .text(response)
                        .build());
        });
    }
}
