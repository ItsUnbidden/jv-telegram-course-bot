package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.BotOrchestrationService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.SessionParamsDto;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.session.UserOrChatRequestSessionService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import java.util.Map;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
@RequiredArgsConstructor
public class CreateBotButtonHandler extends AbstractButtonHandler {
    private static final String IS_BY_ID_PARAM = "isById";

    private final ContentSessionService contentSessionService;
    private final UserOrChatRequestSessionService userOrChatRequestSessionService;
    
    private final BotOrchestrationService botService;

    private final ReplyKeyboardRemove keyboardRemove;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    private final ValidatorUtil validatorUtil;

    @Override
    @Security(authorities = AuthorityType.BOTS_SETTINGS, isBotLordOnly = true)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        if (Boolean.valueOf(params.get(IS_BY_ID_PARAM))) {
            contentSessionService.createSession(user, bot, p -> {
                validatorUtil.checkExactExpectedMessages(user, p.messages(), 2);
                
                botService.createBot(user, entityUtil.getUser(validatorUtil.parseId(user, p.messages().getFirst()),
                        user.getLanguageCode()), p.messages().getLast().getText());
            });
        } else {
            clientManager.getBotLordClient().sendMessage(user, localizationLoader
                    .localize(Localizations.Service.CREATE_BOT_CREATOR_BY_ID_REQUEST, user));

            final KeyboardButtonRequestUser requestUser = KeyboardButtonRequestUser.builder()
                    .userIsBot(false)
                    .requestId(String.valueOf(userOrChatRequestSessionService
                        .createSession(user, bot, getCreateBotFunction(user, bot)))).build();
            final KeyboardButton button = KeyboardButton.builder()
                    .requestUser(requestUser)
                    .text(localizationLoader.localize(Localizations.Button.CHOOSE_USER, user).getData())
                    .build();

            final KeyboardRow row = new KeyboardRow();

            row.add(button);

            final ReplyKeyboardMarkup markup = ReplyKeyboardMarkup.builder()
                    .resizeKeyboard(true)
                    .keyboardRow(row)
                    .build();

            clientManager.getBotLordClient().sendMessage(user, localizationLoader
                    .localize(Localizations.Service.CREATE_BOT_CHOOSE_CREATOR, user), markup);
        }
    }

    private Consumer<SessionParamsDto> getCreateBotFunction(UserEntity director, Bot botlord) {
        return p -> {
            final UserEntity creator = entityUtil.getUser(p.messages().getFirst().getUserShared().getUserId(),
                    director.getLanguageCode());

            contentSessionService.createSession(director, botlord, p2 -> {
                validatorUtil.checkExactExpectedMessages(p2.user(), p2.messages(), 1);
                botService.createBot(director, creator, p2.messages().get(0).getText());
            });

            clientManager.getBotLordClient().sendMessage(director, localizationLoader.localize(
                    Localizations.Service.CREATE_BOT_TOKEN_ONLY_REQUEST, director), keyboardRemove);
        };
    }
}
