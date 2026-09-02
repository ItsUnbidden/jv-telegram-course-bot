package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.SessionParamsDto;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.UserOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.session.UserOrChatRequestSessionService;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import java.util.Map;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
@RequiredArgsConstructor
public class GeneralBanButtonHandler extends AbstractButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(GeneralBanButtonHandler.class);

    private static final String IS_BY_ID_PARAM = "isById";
    private static final String IS_GIVE_BAN_PARAM = "isGiveBan";

    private static final int MAX_BAN_HOURS = 720;

    private final ContentSessionService contentSessionService;
    private final UserOrChatRequestSessionService userOrChatRequestSessionService;

    private final UserOrchestrationService userService;

    private final LocalizationLoader loader;

    private final ReplyKeyboardRemove keyboardRemove;

    private final ClientManager clientManager;

    private final ValidatorUtil validatorUtil;

    @Override
    @Security(authorities = AuthorityType.GENERAL_BANS, isBotLordOnly = true)
    public void handle(BotRole botRole, Map<String, String> params) {
        final boolean isGiveBan = Boolean.valueOf(params.get(IS_GIVE_BAN_PARAM));

        if (Boolean.valueOf(params.get(IS_BY_ID_PARAM))) {
            LOGGER.info("Director " + botRole.getUser().getId() + " wants to ban user by id.");
            contentSessionService.createSession(botRole, p -> {
                final long userId;
            
                if (isGiveBan) {
                    validatorUtil.checkExactExpectedMessages(p.botRole(), p.messages(), 2);
                    userId = validatorUtil.parseId(p.botRole(), p.messages().getFirst());

                    userService.banUserGenerally(p.botRole(), userId, validatorUtil.parseIntInBounds(
                            p.botRole(), p.messages().getLast(), 0, MAX_BAN_HOURS));
                } else {
                    validatorUtil.checkExactExpectedMessages(p.botRole(), p.messages(), 1);
                    userId = validatorUtil.parseId(p.botRole(), p.messages().getFirst());

                    userService.liftGeneralBan(p.botRole(), userId);
                }
            });

            LOGGER.debug("Sending request message to director " + botRole.getUser().getId() + "...");
            if (isGiveBan) {
                clientManager.sendMessage(botRole, loader.localize(Localizations.Service.BAN_USER_ID_REQUEST, botRole));
            } else {
                clientManager.sendMessage(botRole, loader.localize(Localizations.Service.LIFT_BAN_USER_ID_REQUEST, botRole));
            }
            LOGGER.debug("Message sent.");
        } else {
            LOGGER.info("Director " + botRole.getUser().getId() + " wants to ban user by selecting them.");
            final var chooseUserSession = userOrChatRequestSessionService.createSession(botRole, getBanFunction(!isGiveBan));
            final KeyboardButtonRequestUser requestUser = KeyboardButtonRequestUser.builder()
                    .userIsBot(false)
                    .requestId(String.valueOf(chooseUserSession.getRequestId())).build();
            final KeyboardButton button = KeyboardButton.builder()
                    .requestUser(requestUser)
                    .text(loader.localize(Localizations.Button.BAN_CHOOSE_USER, botRole).getData())
                    .build();
            final KeyboardRow row = new KeyboardRow();

            row.add(button);

            final ReplyKeyboardMarkup markup = ReplyKeyboardMarkup.builder()
                    .resizeKeyboard(true)
                    .keyboardRow(row)
                    .build();

            LOGGER.debug("Sending keyboard message to director " + botRole.getUser().getId()
                    + " so that they can choose the target.");
            clientManager.sendMessage(botRole, loader.localize(Localizations.Service.BAN_CHOOSE_USER_REQUEST, botRole), markup);
            LOGGER.debug("Keyboard message sent.");
        }
    }

    private Consumer<SessionParamsDto> getBanFunction(boolean lift) {
        return p -> {
            if (lift) {
                userService.liftGeneralBan(p.botRole(), p.messages().getFirst().getUserShared().getUserId());
                return;
            }

            contentSessionService.createSession(p.botRole(), p2 -> {
                validatorUtil.checkExactExpectedMessages(p2.botRole(), p2.messages(), 1);
                userService.banUserGenerally(p2.botRole(), p.messages().getFirst().getUserShared().getUserId(),
                        validatorUtil.parseIntInBounds(p2.botRole(), p2.messages().getFirst(), 0, MAX_BAN_HOURS));
            });

            LOGGER.debug("Sending request for ban hours to director " + p.botRole().getUser().getId() + "...");
            clientManager.sendMessage(p.botRole(), loader.localize(Localizations.Service.BAN_CHOOSE_USER_HOURS_REQUEST,
                    p.botRole()), keyboardRemove);
            LOGGER.debug("Message sent.");
        };
    }
}
