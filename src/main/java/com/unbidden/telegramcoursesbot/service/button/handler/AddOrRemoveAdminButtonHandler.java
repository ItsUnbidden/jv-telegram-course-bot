package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.command.CommandHandlerManager;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.UserOrChatRequestSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.UserShared;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
@RequiredArgsConstructor
public class AddOrRemoveAdminButtonHandler implements ButtonHandler {
    private static final String PARAM_TARGET_FIRST_NAME = "${targetFirstName}";
    private static final String PARAM_USER_ID = "${userId}";
    
    private static final String SERVICE_REMOVED_ADMIN_NOTIFICATION =
            "service_removed_admin_notification";
    private static final String SERVICE_ADMIN_REMOVE_SUCCESS = "service_admin_remove_success";
    private static final String SERVICE_NEW_ADMIN_NOTIFICATION = "service_new_admin_notification";
    private static final String SERVICE_NEW_ADMIN_ASSIGN_SUCCESS =
            "service_new_admin_assign_success";
    private static final String SERVICE_ADMIN_CHOOSE_ACTION = "service_admin_choose_action";

    private static final String ERROR_ADMIN_REMOVE_FAILURE = "error_admin_remove_failure";
    private static final String ERROR_NEW_ADMIN_ASSIGN_FAILURE = "error_new_admin_assign_failure";

    private static final String BUTTON_REMOVE_ADMIN = "button_remove_admin";
    private static final String BUTTON_ADD_NEW_ADMIN = "button_add_new_admin";

    private final LocalizationLoader localizationLoader;

    private final UserOrChatRequestSessionService sessionService;
    
    private final UserService userService;
    
    private final ReplyKeyboardRemove keyboardRemove;
    
    private final CommandHandlerManager commandHandlerManager;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            KeyboardButtonRequestUser requestUserAddAdmin = KeyboardButtonRequestUser.builder()
                    .userIsBot(false)
                    .requestId(String.valueOf(sessionService.createSession(user,
                        getAddAdminFunction(user)))).build();
            KeyboardButtonRequestUser requestUserRemoveAdmin = KeyboardButtonRequestUser.builder()
                    .userIsBot(false)
                    .requestId(String.valueOf(sessionService.createSession(user,
                        getRemoveAdminFunction(user)))).build();

            KeyboardButton addButton = KeyboardButton.builder()
                    .requestUser(requestUserAddAdmin)
                    .text(localizationLoader.getLocalizationForUser(BUTTON_ADD_NEW_ADMIN, user)
                        .getData())
                    .build();
            KeyboardButton removeButton = KeyboardButton.builder()
                    .requestUser(requestUserRemoveAdmin)
                    .text(localizationLoader.getLocalizationForUser(BUTTON_REMOVE_ADMIN, user)
                        .getData())
                    .build();
            KeyboardRow row = new KeyboardRow();
            row.add(addButton);
            row.add(removeButton);
            ReplyKeyboardMarkup markup = ReplyKeyboardMarkup.builder()
                    .resizeKeyboard(true)
                    .keyboardRow(row)
                    .build();
            final Localization localization = localizationLoader.getLocalizationForUser(
                    SERVICE_ADMIN_CHOOSE_ACTION, user);
            client.sendMessage(user, localization, markup);
        }
    }

    private Consumer<List<Message>> getAddAdminFunction(final UserEntity sender) {
        return m -> {
            final UserShared sharedUser = m.get(0).getUserShared();
            final UserEntity newAdmin = userService.addAdmin(sharedUser.getUserId());

            final Localization error = localizationLoader.getLocalizationForUser(
                    ERROR_NEW_ADMIN_ASSIGN_FAILURE, sender, PARAM_USER_ID,
                    sharedUser.getUserId());
            Localization success = null;
            Localization notification = null;
            if (newAdmin != null) {
                client.setUpMenuForAdmin(newAdmin, commandHandlerManager.getAllCommands());
                success = localizationLoader.getLocalizationForUser(
                        SERVICE_NEW_ADMIN_ASSIGN_SUCCESS, sender, PARAM_TARGET_FIRST_NAME,
                        newAdmin.getFirstName());
                notification = localizationLoader.getLocalizationForUser(
                        SERVICE_NEW_ADMIN_NOTIFICATION, newAdmin);
            }
            sendMessages(sender, newAdmin, error, success, notification);
        };
    }

    private Consumer<List<Message>> getRemoveAdminFunction(final UserEntity sender) {
        return m -> {
            final UserShared sharedUser = m.get(0).getUserShared();
            final UserEntity removedAdmin = userService.removeAdmin(sharedUser.getUserId());

            final Localization error = localizationLoader.getLocalizationForUser(
                    ERROR_ADMIN_REMOVE_FAILURE, sender, PARAM_USER_ID,
                    sharedUser.getUserId());
            Localization success = null;
            Localization notification = null;
            if (removedAdmin != null) {
                client.deleteAdminMenuForUser(removedAdmin);
                success = localizationLoader.getLocalizationForUser(
                        SERVICE_ADMIN_REMOVE_SUCCESS, sender, PARAM_TARGET_FIRST_NAME,
                        removedAdmin.getFirstName());
                notification = localizationLoader.getLocalizationForUser(
                        SERVICE_REMOVED_ADMIN_NOTIFICATION, removedAdmin);
            }
            sendMessages(sender, removedAdmin, error, success, notification);
        };
    }

    private void sendMessages(UserEntity sender, UserEntity target, Localization error,
            Localization success, Localization notification) {
        if (target == null) {
            client.sendMessage(sender, error, keyboardRemove);
            return;
        }

        client.sendMessage(sender, success, keyboardRemove);                            
        client.sendMessage(target, notification);                            
    }
}
