package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.SessionParamsDto;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.UserOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.UserOrChatRequestSessionService;

import java.util.Map;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
@RequiredArgsConstructor
public class SetRoleButtonHandler extends AbstractButtonHandler {
    private static final String ROLE_TYPE_PARAM = "terminal";

    private static final Logger LOGGER = LogManager.getLogger(
            SetRoleButtonHandler.class);
    
    private final LocalizationLoader localizationLoader;

    private final UserOrChatRequestSessionService sessionService;
    
    private final UserOrchestrationService userService;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.ROLE_SETTINGS)
    public void handle(BotRole botRole, Map<String, String> params) {
        final RoleType roleType = RoleType.valueOf(params.get(ROLE_TYPE_PARAM));

        final var chooseUserSession = sessionService.createSession(botRole, getSetRoleFunction(roleType));
        final KeyboardButtonRequestUser requestUserSetRole = KeyboardButtonRequestUser.builder()
                .userIsBot(false)
                .requestId(String.valueOf(chooseUserSession.getRequestId())).build();
        final KeyboardButton addButton = KeyboardButton.builder()
                .requestUser(requestUserSetRole)
                .text(localizationLoader.localize(Localizations.Button.SET_ROLE_CHOOSE_USER, botRole)
                    .getData())
                .build();
        final KeyboardRow row = new KeyboardRow();

        row.add(addButton);

        final ReplyKeyboardMarkup markup = ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .keyboardRow(row)
                .build();
                
        LOGGER.debug("Sending keyboard message to user " + botRole.getUser().getId()
                + " in order for them to choose the target.");
        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.SET_ROLE_USER_REQUEST, botRole), markup);
        LOGGER.debug("Keyboard message sent.");
    }

    private Consumer<SessionParamsDto> getSetRoleFunction(RoleType roleType) {
        return p -> {
            userService.setRole(p.botRole(), p.messages().getFirst().getUserShared().getUserId(), roleType);
        };
    }
}
