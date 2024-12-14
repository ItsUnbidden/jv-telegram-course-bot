package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.session.UserOrChatRequestSessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
@RequiredArgsConstructor
public class BanButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(BanButtonHandler.class);

    private static final String GIVE_BAN = "gb";
    private static final String CHOOSE_USER = "chu";

    private static final String PARAM_HOURS = "${hours}";
    private static final String PARAM_USER_ID = "${userId}";

    private static final String SERVICE_BAN_CHOOSE_USER_HOURS_REQUEST =
            "service_ban_choose_user_hours_request";
    private static final String SERVICE_LIFT_BAN_USER_ID_REQUEST =
            "service_lift_ban_user_id_request";
    private static final String SERVICE_BAN_USER_ID_REQUEST = "service_ban_user_id_request";
    private static final String SERVICE_BAN_CHOOSE_USER_REQUEST =
            "service_ban_choose_user_request";
    private static final String SERVICE_BAN_LIFTED_SUCCESS = "service_ban_lifted_success";
    private static final String SERVICE_BAN_SUCCESS = "service_ban_success";

    private static final String BUTTON_BAN_CHOOSE_USER = "button_ban_choose_user";

    private static final String ERROR_PARSE_HOURS = "error_parse_hours";
    private static final String ERROR_WRONG_BAN_HOURS = "error_wrong_ban_hours";
    private static final String ERROR_USER_ID_LESS_THAN_ZERO = "error_user_id_less_than_zero";
    private static final String ERROR_PARSE_USER_ID = "error_parse_user_id";

    private static final int NUMBER_OF_MESSAGES_EXPECTED_FOR_BAN_ID = 2;
    private static final int NUMBER_OF_MESSAGES_EXPECTED = 1;
    private static final int MAX_BAN_HOURS = 720;

    private final ContentSessionService contentSessionService;
    private final UserOrChatRequestSessionService userOrChatRequestSessionService;

    private final UserService userService;

    private final TextUtil textUtil;

    private final LocalizationLoader localizationLoader;

    private final ReplyKeyboardRemove keyboardRemove;

    private final ClientManager clientManager;
    
    @Override
    @Security(authorities = AuthorityType.BOT_USER_BANS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {

        switch (params[2]) {
            case CHOOSE_USER:
                LOGGER.debug("User " + user.getId() + " wants to choose user.");
                final KeyboardButtonRequestUser requestUser = KeyboardButtonRequestUser.builder()
                        .userIsBot(false)
                        .requestId(String.valueOf(userOrChatRequestSessionService
                            .createSession(user, bot, getBanFunction(user, bot, !params[1]
                            .equals(GIVE_BAN))))).build();
                final KeyboardButton button = KeyboardButton.builder()
                        .requestUser(requestUser)
                        .text(localizationLoader.getLocalizationForUser(BUTTON_BAN_CHOOSE_USER,
                            user).getData())
                        .build();

                final KeyboardRow row = new KeyboardRow();
                row.add(button);
                final ReplyKeyboardMarkup markup = ReplyKeyboardMarkup.builder()
                        .resizeKeyboard(true)
                        .keyboardRow(row)
                        .build();
                LOGGER.debug("Sending keyboard message to user " + user.getId()
                        + " in order for them to choose the target.");
                clientManager.getClient(bot).sendMessage(user, localizationLoader
                        .getLocalizationForUser(SERVICE_BAN_CHOOSE_USER_REQUEST, user),
                        markup);
                LOGGER.debug("Keyboard message sent.");
                break;
            default:
                LOGGER.debug("User " + user.getId() + " wants to input user id.");
                contentSessionService.createSession(user, bot, m -> {
                    textUtil.checkExpectedMessages((params[1].equals(GIVE_BAN))
                            ? NUMBER_OF_MESSAGES_EXPECTED_FOR_BAN_ID
                            : NUMBER_OF_MESSAGES_EXPECTED, user, m, localizationLoader);

                    final long userId;
                    try {
                        userId = Long.parseLong(m.get(0).getText().trim());
                        if (userId <= 0) {
                            throw new InvalidDataSentException("User id must be bigger than 0",
                                    localizationLoader.getLocalizationForUser(
                                    ERROR_USER_ID_LESS_THAN_ZERO, user,
                                    PARAM_USER_ID, userId));
                        }
                        LOGGER.debug("User id has been parsed.");
                    } catch (NumberFormatException e) {
                        throw new InvalidDataSentException("Unable to parse provided string "
                                + m.get(0).getText().trim() + " to user id long",
                                localizationLoader.getLocalizationForUser(ERROR_PARSE_USER_ID,
                                user), e);
                    }
                    switch (params[1]) {
                        case GIVE_BAN:
                            giveBan(user, userService.getUser(userId, user), bot,
                                    getHours(user, m.get(1)));
                            break;
                        default:
                            liftBan(user, userService.getUser(userId, user), bot);
                            break;
                    }
                    LOGGER.debug("Message sent.");
                });
                LOGGER.debug("Sending request message to user " + user.getId() + "...");
                switch (params[1]) {
                    case GIVE_BAN:
                        clientManager.getClient(bot).sendMessage(user, localizationLoader
                                .getLocalizationForUser(SERVICE_BAN_USER_ID_REQUEST, user));
                        break;
                    default:
                        clientManager.getClient(bot).sendMessage(user, localizationLoader
                                .getLocalizationForUser(SERVICE_LIFT_BAN_USER_ID_REQUEST, user));
                        break;
                }
                LOGGER.debug("Message sent.");
                break;
        }
    }

    private Consumer<List<Message>> getBanFunction(UserEntity user, Bot bot, boolean lift) {
        return m -> {
            final UserEntity target = userService.getUser(m.get(0).getUserShared().getUserId(),
                    user);
            
            if (lift) {
                liftBan(user, target, bot);
                return;
            }
            contentSessionService.createSession(user, bot, m2 -> {
                textUtil.checkExpectedMessages(NUMBER_OF_MESSAGES_EXPECTED, user, m2,
                        localizationLoader);
                giveBan(user, target, bot, getHours(user, m2.get(0)));
            });
            LOGGER.debug("Sending request for ban hours to user " + user.getId() + "...");
            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_BAN_CHOOSE_USER_HOURS_REQUEST, user),
                    keyboardRemove);
            LOGGER.debug("Message sent.");
        };
    }

    private int getHours(UserEntity user, Message message) {
        final int hours;
        try {
            hours = Integer.parseInt(message.getText().trim());
            if (hours > MAX_BAN_HOURS) {
                throw new InvalidDataSentException("Hours must less than "
                        + "or equal to " + MAX_BAN_HOURS, localizationLoader
                        .getLocalizationForUser(ERROR_WRONG_BAN_HOURS, user,
                        PARAM_HOURS, hours));
            }
            LOGGER.debug("Hours have been parsed.");
        } catch (NumberFormatException e) {
            throw new InvalidDataSentException("Unable to parse provided string "
                    + message.getText() + " to hours int", localizationLoader
                    .getLocalizationForUser(ERROR_PARSE_HOURS, user), e);
        }
        return hours;
    }

    private void liftBan(UserEntity user, UserEntity target, Bot bot) {
        LOGGER.info("User " + user.getId() + " wants to lift ban from user "
                + target.getId() + " in bot " + bot.getName() + ".");
        userService.liftBanInBot(user, target, bot);
        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_BAN_LIFTED_SUCCESS, user), keyboardRemove);
    }

    private void giveBan(UserEntity user, UserEntity target, Bot bot, int hours) {
        LOGGER.info("User " + user.getId() + " wants to ban user "
                + target.getId() + " in bot " + bot.getName() + ".");

        userService.banUserInBot(user, target, bot, hours);
        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_BAN_SUCCESS, user));
    }
}
