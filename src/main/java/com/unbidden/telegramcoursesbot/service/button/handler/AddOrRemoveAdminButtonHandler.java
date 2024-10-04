package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.SessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.UserShared;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
@RequiredArgsConstructor
public class AddOrRemoveAdminButtonHandler implements ButtonHandler {
    private final TelegramBot bot;

    private final LocalizationLoader localizationLoader;

    private final SessionService sessionService;

    private final UserService userService;

    private final ReplyKeyboardRemove keyboardRemove;

    @Override
    public void handle(String[] params, User user) {
        if (userService.isAdmin(user)) {
            KeyboardButtonRequestUser requestUserAddAdmin = KeyboardButtonRequestUser.builder()
                    .userIsBot(false)
                    .requestId(String.valueOf(sessionService.createSession(user,
                    getAddAdminFunction(user), true))).build();
            KeyboardButtonRequestUser requestUserRemoveAdmin = KeyboardButtonRequestUser.builder()
                    .userIsBot(false)
                    .requestId(String.valueOf(sessionService.createSession(user,
                    getRemoveAdminFunction(user), true))).build();

            KeyboardButton addButton = KeyboardButton.builder()
                    .requestUser(requestUserAddAdmin)
                    .text(localizationLoader.getLocalizationForUser("button_add_new_admin", user)
                        .getData())
                    .build();
            KeyboardButton removeButton = KeyboardButton.builder()
                    .requestUser(requestUserRemoveAdmin)
                    .text(localizationLoader.getLocalizationForUser("button_remove_admin", user)
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
                    "service_admin_choose_action", user);
            bot.sendMessage(SendMessage.builder()
                    .text(localization.getData())
                    .chatId(user.getId())
                    .entities(localization.getEntities())
                    .replyMarkup(markup)
                    .build());
        }
    }

    private Consumer<Message> getAddAdminFunction(final User sender) {
        return m -> {
            final UserShared sharedUser = m.getUserShared();
            final UserEntity newAdmin = userService.addAdmin(sharedUser.getUserId());

            final Localization error = localizationLoader.getLocalizationForUser(
                    "error_new_admin_assign_failure", sender, "${userId}",
                    sharedUser.getUserId());
            Localization success = null;
            Localization notification = null;
            if (newAdmin != null) {
                success = localizationLoader.getLocalizationForUser(
                        "service_new_admin_assign_success", sender, "${targetFirstName}",
                        newAdmin.getFirstName());
                notification = localizationLoader.getLocalizationForUser(
                        "service_new_admin_notification", newAdmin);
            }
            sendMessages(sender, newAdmin, error, success, notification);
        };
    }

    private Consumer<Message> getRemoveAdminFunction(final User sender) {
        return m -> {
            final UserShared sharedUser = m.getUserShared();
            final UserEntity removedAdmin = userService.removeAdmin(sharedUser.getUserId());

            final Localization error = localizationLoader.getLocalizationForUser(
                    "error_admin_remove_failure", sender, "${userId}",
                    sharedUser.getUserId());
            Localization success = null;
            Localization notification = null;
            if (removedAdmin != null) {
                bot.removeMenusForUser(removedAdmin.getId());
                success = localizationLoader.getLocalizationForUser(
                        "service_admin_remove_success", sender, "${targetFirstName}",
                        removedAdmin.getFirstName());
                notification = localizationLoader.getLocalizationForUser(
                        "service_removed_admin_notification", removedAdmin);
            }
            sendMessages(sender, removedAdmin, error, success, notification);
        };
    }

    private void sendMessages(User sender, UserEntity target, Localization error,
            Localization success, Localization notification) {
        if (target == null) {
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
