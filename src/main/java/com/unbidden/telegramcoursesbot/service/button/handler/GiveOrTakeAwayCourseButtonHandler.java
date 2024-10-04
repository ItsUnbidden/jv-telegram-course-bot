package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.CourseBoughtException;
import com.unbidden.telegramcoursesbot.exception.CourseIsAlreadyOwnedException;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.PaymentDetails;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.service.session.SessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.UserShared;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
@RequiredArgsConstructor
public class GiveOrTakeAwayCourseButtonHandler implements ButtonHandler {
    private final TelegramBot bot;

    private final LocalizationLoader localizationLoader;

    private final UserService userService;

    private final SessionService sessionService;

    private final PaymentService paymentService;

    private final ReplyKeyboardRemove keyboardRemove;

    private final CourseService courseService;

    @Override
    public void handle(String[] params, User user) {
        if (userService.isAdmin(user)) {
            Localization localization = null;
            ReplyKeyboard markup = keyboardRemove;

            try {
                final Course course = courseService.getCourseByName(params[0]);

                KeyboardButtonRequestUser requestUserGiveCourse = KeyboardButtonRequestUser
                        .builder()
                        .userIsBot(false)
                        .requestId(String.valueOf(sessionService.createSession(user,
                            getGiveCourseFunction(user, course), true))).build();
                KeyboardButtonRequestUser requestUserTakeCourse = KeyboardButtonRequestUser
                        .builder()
                        .userIsBot(false)
                        .requestId(String.valueOf(sessionService.createSession(user,
                            getTakeCourseFunction(user, course), true))).build();

                KeyboardButton giveButton = KeyboardButton.builder()
                        .requestUser(requestUserGiveCourse)
                        .text(localizationLoader.getLocalizationForUser("button_give_course",
                            user).getData())
                        .build();
                KeyboardButton takeButton = KeyboardButton.builder()
                        .requestUser(requestUserTakeCourse)
                        .text(localizationLoader.getLocalizationForUser("button_take_course",
                            user).getData())
                        .build();
                KeyboardRow row = new KeyboardRow();
                row.add(giveButton);
                row.add(takeButton);
                markup = ReplyKeyboardMarkup.builder()
                        .keyboardRow(row)
                        .resizeKeyboard(true)
                        .build();
                localization = localizationLoader.getLocalizationForUser(
                        "service_give_take_course_choose_action", user,
                        "${courseName}", params[0]);
            } catch (EntityNotFoundException e) {
                localization = localizationLoader.getLocalizationForUser(
                    "error_give_take_course_no_longer_available", user,
                    "${courseName}", params[0]);
            }
            
            bot.sendMessage(SendMessage.builder()
                    .text(localization.getData())
                    .chatId(user.getId())
                    .entities(localization.getEntities())
                    .replyMarkup(markup)
                    .build());
        }
    }

    private Consumer<Message> getGiveCourseFunction(final User sender, final Course course) {
        return m -> {
            final UserShared sharedUser = m.getUserShared();
            final Map<String, Object> parametersMap = new HashMap<>();
                    parametersMap.put("${courseName}", course.getName());
                    parametersMap.put("${targetId}", course.getName());

            UserEntity newOwner = null;
            Localization success = null;
            Localization notification = null;
            Localization error = null;

            try {
                newOwner = userService.getUser(sharedUser.getUserId());
                if (paymentService.isAvailable(newOwner, course.getName())) {
                    throw new CourseIsAlreadyOwnedException("Course " + course
                            + " is alredy owned by user " + newOwner.getId());
                }
                final PaymentDetails paymentDetails = new PaymentDetails();
                paymentDetails.setGifted(true);
                paymentDetails.setCourse(course);
                paymentDetails.setSuccessful(true);
                paymentDetails.setTelegramPaymentChargeId("Not inluded");
                paymentDetails.setTotalAmount(0);
                paymentDetails.setUser(newOwner);
                paymentDetails.setValid(true);
                paymentService.addPaymentDetails(paymentDetails);

                parametersMap.put("${targetFirstName}", newOwner.getFirstName());
                success = localizationLoader.getLocalizationForUser(
                        "service_course_gifted_successfuly", sender, parametersMap);
                notification = localizationLoader.getLocalizationForUser(
                "service_course_gifted_notification", newOwner, parametersMap);
            } catch (EntityNotFoundException e1) {
                error = localizationLoader.getLocalizationForUser("error_give_course_user_"
                        + "not_found", sender, parametersMap);
            } catch (CourseIsAlreadyOwnedException e2) {
                final UserEntity supposedUser = userService.getUser(sharedUser.getUserId());

                parametersMap.put("${targetFirstName}", supposedUser.getFirstName());
                error = localizationLoader.getLocalizationForUser("error_give_course_already"
                        + "_owned", sender, parametersMap);
            }
            sendMessages(sender, newOwner, error, success, notification);
        };
    }

    private Consumer<Message> getTakeCourseFunction(final User sender, final Course course) {
        return m -> {
            final UserShared sharedUser = m.getUserShared();
            final Map<String, Object> parametersMap = new HashMap<>();
                    parametersMap.put("${courseName}", course.getName());
                    parametersMap.put("${targetId}", course.getName());

            UserEntity pastOwner = null;
            Localization success = null;
            Localization notification = null;
            Localization error = null;

            try {
                pastOwner = userService.getUser(sharedUser.getUserId());
                if (!paymentService.isAvailableAndGifted(pastOwner, course.getName())) {
                    throw new CourseBoughtException("Course " + course
                            + " either is not owned by user " + pastOwner.getId() + " at all or "
                            + "has been bought by them. Only gifted courses can be taken away.");
                }
                paymentService.deleteByCourseForUser(course.getName(), pastOwner.getId());

                parametersMap.put("${targetFirstName}", pastOwner.getFirstName());
                success = localizationLoader.getLocalizationForUser(
                        "service_course_taken_successfuly", sender, parametersMap);
                notification = localizationLoader.getLocalizationForUser(
                "service_course_taken_notification", pastOwner, parametersMap);
            } catch (EntityNotFoundException e1) {
                error = localizationLoader.getLocalizationForUser("error_take_course_user_"
                        + "not_found", sender, parametersMap);
            } catch (CourseBoughtException e2) {
                final UserEntity supposedUser = userService.getUser(sharedUser.getUserId());
                
                parametersMap.put("${targetFirstName}", supposedUser.getFirstName());
                error = localizationLoader.getLocalizationForUser(
                        "error_take_course_bought_or_missing", sender, parametersMap);
            }
            sendMessages(sender, pastOwner, error, success, notification);
        };
    }

    private void sendMessages(User sender, UserEntity target, Localization error,
            Localization success, Localization notification) {
        if (error != null) {
            bot.sendMessage(SendMessage.builder()
                    .chatId(sender.getId())
                    .text(error.getData())
                    .entities(error.getEntities())
                    .replyMarkup(keyboardRemove)
                    .build());
            return;
        }

        bot.sendMessage(SendMessage.builder()
            .chatId(sender.getId())
            .text(success.getData())
            .entities(success.getEntities())
            .replyMarkup(keyboardRemove)
            .build());                            
        bot.sendMessage(SendMessage.builder()
            .chatId(target.getId())
            .text(notification.getData())
            .entities(notification.getEntities())
            .build());                            
    }   
}
