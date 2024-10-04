package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.SessionService;
import com.unbidden.telegramcoursesbot.util.Blockable;
import java.util.HashMap;
import java.util.Map;
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

    private final CourseService courseService;

    private final SessionService sessionService;

    private final LocalizationLoader localizationLoader;

    @Override
    @Blockable
    public void handle(String[] params, User user) {
        final Course course = courseService.getCourseByName(params[0]);
        final Map<String, Object> messageParams = new HashMap<>();
        
        messageParams.put("${courseName}", course.getName());
        messageParams.put("${currentPrice}", course.getPrice());

        LOGGER.info("Course price change handler was triggered. Current value is: "
                + course.getPrice() + ".");
        sessionService.createSession(user, (m) -> {
            Localization response = localizationLoader.getLocalizationForUser(
                "error_new_price_cannot_parse", user);
            if (m.hasText()) {
                try {
                    int newPrice = Integer.parseInt(m.getText().trim());
                    LOGGER.info("New price " + newPrice + " for course " + course.getName()
                            + " parsed successfuly.");
                    course.setPrice(newPrice);
                    courseService.save(course);
                    LOGGER.info("New price saved.");
                    response = localizationLoader.getLocalizationForUser(
                            "service_course_price_update_success", user, messageParams);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Unable to parse new price provided by the user.");
                }
            } else {
                LOGGER.warn("Message provided by the user does not have any text.");
            }
            bot.sendMessage(SendMessage.builder()
                        .chatId(user.getId())
                        .text(response.getData())
                        .entities(response.getEntities())
                        .build());
        }, false);
        Localization updateRequestLocalization = localizationLoader.getLocalizationForUser(
                "service_course_price_update_request", user, messageParams);
        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(updateRequestLocalization.getData())
                .entities(updateRequestLocalization.getEntities())
                .build());
    }
}
