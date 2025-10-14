package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.CourseBoughtException;
import com.unbidden.telegramcoursesbot.exception.CourseIsAlreadyOwnedException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.PaymentDetails;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.service.session.UserOrChatRequestSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.UserShared;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
@RequiredArgsConstructor
public class GiveOrTakeAwayCourseButtonHandler implements ButtonHandler {
    private static final String PARAM_TARGET_TITLE = "${targetTitle}";
    private static final String PARAM_SENDER_TITLE = "${senderTitle}";
    private static final String PARAM_SENDER_FULL_NAME = "${senderFullName}";
    private static final String PARAM_TARGET_FULL_NAME = "${targetFullName}";
    private static final String PARAM_TARGET_ID = "${targetId}";
    private static final String PARAM_COURSE_NAME = "${courseName}";

    private static final String SERVICE_COURSE_TAKEN_NOTIFICATION =
            "service_course_taken_notification";
    private static final String SERVICE_COURSE_TAKEN_SUCCESSFULY =
            "service_course_taken_successfuly";
    private static final String SERVICE_COURSE_GIFTED_NOTIFICATION =
            "service_course_gifted_notification";
    private static final String SERVICE_COURSE_GIFTED_SUCCESSFULY =
            "service_course_gifted_successfuly";
    private static final String SERVICE_GIVE_TAKE_COURSE_CHOOSE_ACTION =
            "service_give_take_course_choose_action";
    
    private static final String ERROR_TAKE_COURSE_BOUGHT_OR_MISSING =
            "error_take_course_bought_or_missing";
    private static final String ERROR_TAKE_COURSE_USER_NOT_FOUND =
            "error_take_course_user_not_found";
    private static final String ERROR_GIVE_COURSE_ALREADY_OWNED =
            "error_give_course_already_owned";
    private static final String ERROR_GIVE_COURSE_USER_NOT_FOUND =
            "error_give_course_user_not_found";
    private static final String ERROR_GIVE_TAKE_COURSE_NO_LONGER_AVAILABLE =
            "error_give_take_course_no_longer_available";

    private static final String COURSE_NAME = "course_%s_name";

    private static final String BUTTON_TAKE_COURSE = "button_take_course";
    private static final String BUTTON_GIVE_COURSE = "button_give_course";

    private final ReplyKeyboardRemove keyboardRemove;
    
    private final UserService userService;
    
    private final UserOrChatRequestSessionService sessionService;
    
    private final PaymentService paymentService;
    
    private final CourseService courseService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.GIVE_COURSE)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        Localization localization = null;
        ReplyKeyboard markup = keyboardRemove;

        try {
            final Course course = courseService.getCourseByName(params[0], user, bot);

            KeyboardButtonRequestUser requestUserGiveCourse = KeyboardButtonRequestUser
                    .builder()
                    .userIsBot(false)
                    .requestId(String.valueOf(sessionService.createSession(user, bot,
                        getGiveCourseFunction(user, course, bot)))).build();
            KeyboardButtonRequestUser requestUserTakeCourse = KeyboardButtonRequestUser
                    .builder()
                    .userIsBot(false)
                    .requestId(String.valueOf(sessionService.createSession(user, bot,
                        getTakeCourseFunction(user, course, bot)))).build();

            KeyboardButton giveButton = KeyboardButton.builder()
                    .requestUser(requestUserGiveCourse)
                    .text(localizationLoader.getLocalizationForUser(BUTTON_GIVE_COURSE,
                        user).getData())
                    .build();
            KeyboardButton takeButton = KeyboardButton.builder()
                    .requestUser(requestUserTakeCourse)
                    .text(localizationLoader.getLocalizationForUser(BUTTON_TAKE_COURSE,
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
                    SERVICE_GIVE_TAKE_COURSE_CHOOSE_ACTION, user,
                    PARAM_COURSE_NAME, localizationLoader.getLocalizationForUser(
                    COURSE_NAME.formatted(params[0]), user).getData());
        } catch (EntityNotFoundException e) {
            localization = localizationLoader.getLocalizationForUser(
                ERROR_GIVE_TAKE_COURSE_NO_LONGER_AVAILABLE, user,
                PARAM_COURSE_NAME, localizationLoader.getLocalizationForUser(
                COURSE_NAME.formatted(params[0]), user).getData());
        }
        
        clientManager.getClient(bot).sendMessage(user, localization, markup);
    }

    private Consumer<List<Message>> getGiveCourseFunction(UserEntity sender,
            Course course, Bot bot) {
        return m -> {
            final UserShared sharedUser = m.get(0).getUserShared();
            final Map<String, Object> parametersMap = new HashMap<>();
            parametersMap.put(PARAM_COURSE_NAME, localizationLoader.getLocalizationForUser(
                    COURSE_NAME.formatted(course.getName()), sender).getData());
            parametersMap.put(PARAM_TARGET_ID, sharedUser.getUserId());
            parametersMap.put(PARAM_SENDER_FULL_NAME, sender.getFullName());
            
            UserEntity newOwner = null;
            Localization success = null;
            Localization notification = null;
            Localization error = null;

            try {
                newOwner = userService.getUser(sharedUser.getUserId(), sender);
                if (paymentService.isAvailable(newOwner, bot, course.getName())) {
                    throw new CourseIsAlreadyOwnedException("Course " + course
                            + " is alredy owned by user " + newOwner.getId());
                }
                final PaymentDetails paymentDetails = new PaymentDetails();
                paymentDetails.setGifted(true);
                paymentDetails.setCourse(course);
                paymentDetails.setBot(bot);
                paymentDetails.setTelegramPaymentChargeId("Not inluded");
                paymentDetails.setTotalAmount(0);
                paymentDetails.setTimestamp(LocalDateTime.now());
                paymentDetails.setUser(newOwner);
                paymentDetails.setValid(true);
                paymentService.addPaymentDetails(paymentDetails);

                parametersMap.put(PARAM_TARGET_FULL_NAME, newOwner.getFullName());
                parametersMap.put(PARAM_SENDER_TITLE, userService.getLocalizedTitle(sender,
                        newOwner, bot));
                parametersMap.put(PARAM_TARGET_TITLE, userService.getLocalizedTitle(newOwner,
                        sender, bot));
                success = localizationLoader.getLocalizationForUser(
                        SERVICE_COURSE_GIFTED_SUCCESSFULY, sender, parametersMap);
                notification = localizationLoader.getLocalizationForUser(
                SERVICE_COURSE_GIFTED_NOTIFICATION, newOwner, parametersMap);
            } catch (EntityNotFoundException e1) {
                error = localizationLoader.getLocalizationForUser(
                        ERROR_GIVE_COURSE_USER_NOT_FOUND, sender, parametersMap);
            } catch (CourseIsAlreadyOwnedException e2) {
                final UserEntity supposedUser = userService.getUser(sharedUser.getUserId(),
                        sender);

                parametersMap.put(PARAM_TARGET_FULL_NAME, supposedUser.getFirstName());
                parametersMap.put(PARAM_TARGET_TITLE, userService.getLocalizedTitle(newOwner,
                        sender, bot));
                error = localizationLoader.getLocalizationForUser(ERROR_GIVE_COURSE_ALREADY_OWNED,
                        sender, parametersMap);
            }
            sendMessages(sender, newOwner, error, success, notification, bot);
        };
    }

    private Consumer<List<Message>> getTakeCourseFunction(UserEntity sender,
            Course course, Bot bot) {
        return m -> {
            final UserShared sharedUser = m.get(0).getUserShared();
            final Map<String, Object> parametersMap = new HashMap<>();
            parametersMap.put(PARAM_COURSE_NAME, localizationLoader.getLocalizationForUser(
                    COURSE_NAME.formatted(course.getName()), sender).getData());
            parametersMap.put(PARAM_TARGET_ID, sharedUser.getUserId());
            parametersMap.put(PARAM_SENDER_FULL_NAME, sender.getFullName());

            UserEntity pastOwner = null;
            Localization success = null;
            Localization notification = null;
            Localization error = null;

            try {
                pastOwner = userService.getUser(sharedUser.getUserId(), sender);
                if (!paymentService.isAvailableAndGifted(pastOwner, bot, course.getName())) {
                    throw new CourseBoughtException("Course " + course
                            + " either is not owned by user " + pastOwner.getId() + " at all or "
                            + "has been bought by them. Only gifted courses can be taken away.");
                }
                paymentService.deleteByCourseForUser(course.getName(), pastOwner.getId());

                parametersMap.put(PARAM_TARGET_FULL_NAME, pastOwner.getFullName());
                parametersMap.put(PARAM_TARGET_TITLE, userService.getLocalizedTitle(pastOwner,
                        sender, bot));
                parametersMap.put(PARAM_SENDER_TITLE, userService.getLocalizedTitle(sender,
                        pastOwner, bot));
                success = localizationLoader.getLocalizationForUser(
                        SERVICE_COURSE_TAKEN_SUCCESSFULY, sender, parametersMap);
                notification = localizationLoader.getLocalizationForUser(
                        SERVICE_COURSE_TAKEN_NOTIFICATION, pastOwner, parametersMap);
            } catch (EntityNotFoundException e1) {
                error = localizationLoader.getLocalizationForUser(
                        ERROR_TAKE_COURSE_USER_NOT_FOUND, sender, parametersMap);
            } catch (CourseBoughtException e2) {
                final UserEntity supposedUser = userService.getUser(sharedUser.getUserId(),
                        sender);
                
                parametersMap.put(PARAM_TARGET_FULL_NAME, supposedUser.getFullName());
                parametersMap.put(PARAM_TARGET_TITLE, userService.getLocalizedTitle(
                        supposedUser, sender, bot));
                error = localizationLoader.getLocalizationForUser(
                        ERROR_TAKE_COURSE_BOUGHT_OR_MISSING, sender, parametersMap);
            }
            sendMessages(sender, pastOwner, error, success, notification, bot);
        };
    }

    private void sendMessages(UserEntity sender, UserEntity target, Localization error,
            Localization success, Localization notification, Bot bot) {
        if (error != null) {
            clientManager.getClient(bot).sendMessage(sender, error, keyboardRemove);
            return;
        }

        clientManager.getClient(bot).sendMessage(sender, success, keyboardRemove);                            
        clientManager.getClient(bot).sendMessage(target, notification);                            
    }   
}
