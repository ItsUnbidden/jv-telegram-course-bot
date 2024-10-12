package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.SessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.Blockable;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class CoursePriceChangeButtonHandler implements ButtonHandler {
    private static final Logger LOGGER =
            LogManager.getLogger(CoursePriceChangeButtonHandler.class);
            
    private static final String PARAM_CURRENT_PRICE = "${currentPrice}";
    private static final String PARAM_COURSE_NAME = "${courseName}";

    private static final String SERVICE_COURSE_PRICE_UPDATE_REQUEST =
            "service_course_price_update_request";
    private static final String SERVICE_COURSE_PRICE_UPDATE_SUCCESS =
            "service_course_price_update_success";

    private static final String ERROR_NEW_PRICE_CANNOT_PARSE = "error_new_price_cannot_parse";

    private final TelegramBot bot;

    private final CourseService courseService;

    private final SessionService sessionService;
    
    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    @Override
    @Blockable
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Course course = courseService.getCourseByName(params[0]);
            final Map<String, Object> messageParams = new HashMap<>();
            
            messageParams.put(PARAM_COURSE_NAME, course.getName());
            messageParams.put(PARAM_CURRENT_PRICE, course.getPrice());
    
            LOGGER.info("Course price change handler was triggered. Current value is: "
                    + course.getPrice() + ".");
            sessionService.createSession(user, false, (m) -> {
                Localization response = localizationLoader.getLocalizationForUser(
                    ERROR_NEW_PRICE_CANNOT_PARSE, user);
                if (m.hasText()) {
                    try {
                        int newPrice = Integer.parseInt(m.getText().trim());
                        LOGGER.info("New price " + newPrice + " for course " + course.getName()
                                + " parsed successfuly.");
                        course.setPrice(newPrice);
                        courseService.save(course);
                        LOGGER.info("New price saved.");
                        response = localizationLoader.getLocalizationForUser(
                                SERVICE_COURSE_PRICE_UPDATE_SUCCESS, user, messageParams);
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
            });
            Localization updateRequestLocalization = localizationLoader.getLocalizationForUser(
                    SERVICE_COURSE_PRICE_UPDATE_REQUEST, user, messageParams);
            bot.sendMessage(SendMessage.builder()
                    .chatId(user.getId())
                    .text(updateRequestLocalization.getData())
                    .entities(updateRequestLocalization.getEntities())
                    .build());
        }
    }
}
