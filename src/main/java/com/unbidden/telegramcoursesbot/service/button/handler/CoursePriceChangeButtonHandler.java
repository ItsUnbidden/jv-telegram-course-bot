package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.Blockable;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoursePriceChangeButtonHandler implements ButtonHandler {
    private static final Logger LOGGER =
            LogManager.getLogger(CoursePriceChangeButtonHandler.class);
            
    private static final String PARAM_CURRENT_PRICE = "${currentPrice}";
    private static final String PARAM_COURSE_NAME = "${courseName}";
    private static final String PARAM_MAX_PRICE = "${maxPrice}";

    private static final String SERVICE_COURSE_PRICE_UPDATE_REQUEST =
            "service_course_price_update_request";
    private static final String SERVICE_COURSE_PRICE_UPDATE_SUCCESS =
            "service_course_price_update_success";

    private static final String ERROR_PRICE_LIMIT = "error_price_limit";
    private static final String ERROR_PARSE_PRICE_FAILURE = "error_parse_price_failure";

    private static final int MAX_PRICE = 100_000;

    private final CourseService courseService;
    
    private final ContentSessionService sessionService;
    
    private final UserService userService;
    
    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    @Blockable
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final Course course = courseService.getCourseByName(params[0], user);
            final Map<String, Object> messageParams = new HashMap<>();
            
            messageParams.put(PARAM_COURSE_NAME, course.getName());
            messageParams.put(PARAM_CURRENT_PRICE, course.getPrice());
    
            LOGGER.info("Course price change handler was triggered. Current value is: "
                    + course.getPrice() + ".");
            sessionService.createSession(user, (m) -> {
                final String providedStr = m.get(0).getText().trim();
                try {
                    final int newPrice = Integer.parseInt(providedStr);
                    LOGGER.info("New price " + newPrice + " for course " + course.getName()
                            + " parsed successfuly.");
                    if (newPrice > MAX_PRICE || newPrice <= 0) {
                        throw new InvalidDataSentException("Price cannot be more then "
                                + MAX_PRICE + " or less than 1", localizationLoader
                                .getLocalizationForUser(ERROR_PRICE_LIMIT, user,
                                PARAM_MAX_PRICE, MAX_PRICE));
                    }
                    course.setPrice(newPrice);
                    courseService.save(course);
                    LOGGER.info("New price saved.");
                    final Localization response = localizationLoader.getLocalizationForUser(
                            SERVICE_COURSE_PRICE_UPDATE_SUCCESS, user, messageParams);

                    client.sendMessage(user, response);
                } catch (NumberFormatException e) {
                    throw new InvalidDataSentException("Unable to parse provided string "
                            + providedStr + " to new price int", localizationLoader
                            .getLocalizationForUser(ERROR_PARSE_PRICE_FAILURE, user), e);
                }
            }, true);
            Localization updateRequestLocalization = localizationLoader.getLocalizationForUser(
                    SERVICE_COURSE_PRICE_UPDATE_REQUEST, user, messageParams);
            client.sendMessage(user, updateRequestLocalization);
        }
    }
}
