package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
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
import java.util.regex.Pattern;
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
public class CreateBotButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(CreateBotButtonHandler.class);

    private static final String PARAM_USER_ID = "${userId}";

    private static final String SERVICE_CREATE_BOT_NAME_TOKEN_REQUEST =
            "service_create_bot_name_token_request";
    private static final String SERVICE_BOT_CREATED_CREATOR_NOTIFICATION =
            "service_bot_created_creator_notification";
    private static final String SERVICE_NEW_BOT_CREATED = "service_new_bot_created";
    private static final String SERVICE_CREATE_BOT_CREATOR_BY_ID_REQUEST =
            "service_create_bot_creator_by_id_request";
    private static final String SERVICE_CREATE_BOT_CHOOSE_CREATOR =
            "service_create_bot_choose_creator_request";

    private static final String BUTTON_CHOOSE_USER = "button_create_bot_choose_user";

    private static final String ERROR_BOT_NAME_PATTERN_MISMATCH =
            "error_bot_name_pattern_mismatch";
    private static final String ERROR_BOT_TOKEN_PATTERN_MISMATCH =
            "error_bot_token_pattern_mismatch";
    private static final String ERROR_BOT_NAME_LENGTH = "error_bot_name_length";
    private static final String ERROR_USER_ID_LESS_THAN_ZERO = "error_user_id_less_than_zero";
    private static final String ERROR_PARSE_USER_ID = "error_parse_user_id";
    private static final String ERROR_BOT_ALREADY_EXISTS = "error_bot_already_exists";
    
    private static final String CHOOSE_USER = "chu";

    private static final int MAX_BOT_NAME_LENGTH = 20;
    private static final int MIN_BOT_NAME_LENGTH = 3;
    private static final int EXPECTED_MESSAGES_BY_ID = 3;
    private static final int EXPECTED_MESSAGES_CHOOSE_USER = 2;
    private static final Pattern BOT_TOKEN_PATTERN = Pattern
            .compile("\\d{1,20}:[a-zA-Z0-9\\-]{1,50}");
    private static final Pattern BOT_NAME_PATTERN = Pattern
            .compile("[a-z0-9_]+");

    private final ContentSessionService contentSessionService;
    private final UserOrChatRequestSessionService userOrChatRequestSessionService;
    
    private final BotService botService;

    private final UserService userService;

    private final TextUtil textUtil;

    private final ReplyKeyboardRemove keyboardRemove;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.BOTS_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        botService.checkBotFather(bot, user);
        switch (params[1]) {
            case CHOOSE_USER:
                LOGGER.debug("User " + user.getId() + " wants to choose user.");
                final KeyboardButtonRequestUser requestUser = KeyboardButtonRequestUser.builder()
                        .userIsBot(false)
                        .requestId(String.valueOf(userOrChatRequestSessionService
                            .createSession(user, bot, getCreateBotFunction(user, bot)))).build();
                final KeyboardButton button = KeyboardButton.builder()
                        .requestUser(requestUser)
                        .text(localizationLoader.getLocalizationForUser(BUTTON_CHOOSE_USER,
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
                clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                        .getLocalizationForUser(SERVICE_CREATE_BOT_CHOOSE_CREATOR, user),
                        markup);
                LOGGER.debug("Keyboard message sent.");
                break;
            default:
                LOGGER.debug("User " + user.getId() + " wants to input user id.");
                contentSessionService.createSession(user, bot, m -> {
                    textUtil.checkExpectedMessages(EXPECTED_MESSAGES_BY_ID, user,
                            m, localizationLoader);

                    final UserEntity creator;
                    try {
                        final long userId = Long.parseLong(m.get(0).getText().trim());
                        if (userId <= 0) {
                            throw new InvalidDataSentException("User id must be bigger than 0",
                                    localizationLoader.getLocalizationForUser(
                                    ERROR_USER_ID_LESS_THAN_ZERO, user,
                                    PARAM_USER_ID, userId));
                        }
                        creator = userService.getUser(userId, user);
                        LOGGER.debug("User has been parsed.");
                    } catch (NumberFormatException e) {
                        throw new InvalidDataSentException("Unable to parse provided string "
                                + m.get(0).getText().trim() + " to user id long",
                                localizationLoader.getLocalizationForUser(ERROR_PARSE_USER_ID,
                                user), e);
                    }
                    
                    createBot(user, creator, m.get(1).getText(), m.get(2).getText());
                });
                LOGGER.debug("Sending request message to user " + user.getId() + "...");
                clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                        .getLocalizationForUser(SERVICE_CREATE_BOT_CREATOR_BY_ID_REQUEST, user));
                LOGGER.debug("Message sent.");
                break;
        }
    }

    private Consumer<List<Message>> getCreateBotFunction(UserEntity user, Bot bot) {
        return m -> {
            final UserEntity creator = userService.getUser(m.get(0).getUserShared().getUserId(),
                    user);
            contentSessionService.createSession(user, bot, m2 -> {
                textUtil.checkExpectedMessages(EXPECTED_MESSAGES_CHOOSE_USER, user, m2,
                        localizationLoader);
                createBot(user, creator, m2.get(0).getText(), m2.get(1).getText());
            });
            LOGGER.debug("Sending request for bot name and token to director...");
            clientManager.getBotFatherClient().sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_CREATE_BOT_NAME_TOKEN_REQUEST, user),
                    keyboardRemove);
            LOGGER.debug("Message sent.");
        };
    }

    private void createBot(UserEntity director, UserEntity creator,
            String rawBotName, String rawToken) {
        final String botName = rawBotName.trim();
        if (botName.length() < MIN_BOT_NAME_LENGTH
                || botName.length() > MAX_BOT_NAME_LENGTH) {
            throw new InvalidDataSentException("Bot name length cannot be shorter "
                    + "than " + MIN_BOT_NAME_LENGTH + " and longer than "
                    + MAX_BOT_NAME_LENGTH, localizationLoader.getLocalizationForUser(
                    ERROR_BOT_NAME_LENGTH, director));
        }
        if (!BOT_NAME_PATTERN.matcher(botName).matches()) {
            throw new InvalidDataSentException("Bot name " + botName
                    + " does not match the bot name pattern", localizationLoader
                    .getLocalizationForUser(ERROR_BOT_NAME_PATTERN_MISMATCH, director));
        }
        try {
            final Bot bot = botService.getBot(botName);
            throw new InvalidDataSentException("Bot " + botName + " already exists under id "
                    + bot.getId(), localizationLoader.getLocalizationForUser(
                    ERROR_BOT_ALREADY_EXISTS, director));
        } catch (EntityNotFoundException e) {
            LOGGER.debug("Bot " + botName + " does not exist yet. Proceeding...");
        }
        LOGGER.debug("Bot name has been parsed.");

        final String botToken = rawToken.trim();
        if (!BOT_TOKEN_PATTERN.matcher(botToken).matches()) {
            throw new InvalidDataSentException("Bot token " + botToken
                    + " does not match the bot token  pattern", localizationLoader
                    .getLocalizationForUser(ERROR_BOT_TOKEN_PATTERN_MISMATCH, director));
        }
        LOGGER.debug("Bot token has been parsed.");

        LOGGER.info("Creating new bot for creator " + creator.getId() + " with name "
                + botName + " and token (REDACTED)...");
        final Bot newBot = botService.createBot(creator, botName, botToken);
        LOGGER.info("New bot " + newBot.getName() + " with id " + newBot.getId()
                + " has been created and initialized.");
        LOGGER.debug("Sending confirmation messages...");
        clientManager.getBotFatherClient().sendMessage(director, localizationLoader
                .getLocalizationForUser(SERVICE_NEW_BOT_CREATED, director));
        clientManager.getClient(newBot).sendMessage(creator, localizationLoader
                .getLocalizationForUser(SERVICE_BOT_CREATED_CREATOR_NOTIFICATION,
                director));
    }
}
