package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.bot.RegularClient;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Role;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.UserOrChatRequestSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
@RequiredArgsConstructor
public class SetRoleButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            SetRoleButtonHandler.class);
        
    private static final String PARAM_NEW_ROLE_TYPE = "${newRoleType}";
    private static final String PARAM_TARGET_FULL_NAME = "${targetFullName}";
    
    private static final String SERVICE_SET_ROLE_USER_REQUEST = "service_set_role_user_request";
    private static final String SERVICE_SET_ROLE_SUCCESS = "service_set_role_success";
    
    private static final String BUTTON_SET_ROLE_CHOOSE_USER = "button_set_role_choose_user";

    private final LocalizationLoader localizationLoader;

    private final UserOrChatRequestSessionService sessionService;
    
    private final UserService userService;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.ROLE_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        final Role newRole = userService.getRole(RoleType.valueOf(params[1]));
        LOGGER.info("User " + user.getId() + " is trying to give role "
                + newRole.getType() + " to a user in bot " + bot.getName() + ".");

        final KeyboardButtonRequestUser requestUserSetRole = KeyboardButtonRequestUser.builder()
                .userIsBot(false)
                .requestId(String.valueOf(sessionService.createSession(user, bot,
                    getSetRoleFunction(user, bot, newRole)))).build();
        final KeyboardButton addButton = KeyboardButton.builder()
                .requestUser(requestUserSetRole)
                .text(localizationLoader.getLocalizationForUser(BUTTON_SET_ROLE_CHOOSE_USER, user)
                    .getData())
                .build();

        final KeyboardRow row = new KeyboardRow();
        row.add(addButton);
        final ReplyKeyboardMarkup markup = ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .keyboardRow(row)
                .build();
        LOGGER.debug("Sending keyboard message to user " + user.getId()
                + " in order for them to choose the target.");
        final Localization localization = localizationLoader.getLocalizationForUser(
                SERVICE_SET_ROLE_USER_REQUEST, user);
        clientManager.getClient(bot).sendMessage(user, localization, markup);
        LOGGER.debug("Keyboard message sent.");
    }

    private Consumer<List<Message>> getSetRoleFunction(UserEntity user, Bot bot, Role role) {
        return m -> {
            final UserEntity target = userService.getUser(m.get(0).getUserShared().getUserId(),
                    user);
            userService.setRole(user, target, bot, role);
            LOGGER.info("User " + target.getId() + " is now of " + role.getType()
                    + " role in bot " + bot.getName() + ". Sending confirmation...");
            final RegularClient client = (RegularClient)clientManager.getClient(bot);

            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put(PARAM_TARGET_FULL_NAME, target.getFullName());
            parameterMap.put(PARAM_NEW_ROLE_TYPE, role.getType());

            client.sendMessage(user, localizationLoader.getLocalizationForUser(
                    SERVICE_SET_ROLE_SUCCESS, user, parameterMap));
            LOGGER.info("Message sent. Setting new menus for user " + target.getId() + "...");
            client.removeMenuForUser(target);
            client.setUpMenuForUserForRole(target, role);
            LOGGER.info("Menus have been set up for user "+ target.getId() + ".");
        };
    }
}
