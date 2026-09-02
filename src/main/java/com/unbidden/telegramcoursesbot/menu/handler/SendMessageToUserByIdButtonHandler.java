package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.post.PostService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendMessageToUserByIdButtonHandler extends AbstractButtonHandler {
    private final ContentSessionService sessionService;

    private final PostService postService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    private final ValidatorUtil validatorUtil;

    @Override
    @Security(authorities = AuthorityType.POST)
    public void handle(BotRole botRole, Map<String, String> params) {
        sessionService.createSession(botRole, p -> {
            validatorUtil.checkExactExpectedMessages(p.botRole(), p.messages(), 1);
            final long userId = validatorUtil.parseId(p.botRole(), p.messages().getFirst());
            final BotRole targetRole = entityUtil.getActiveBotRole(botRole, userId);

            requestContentAndSendMessage(p.botRole(), targetRole);
        }, true);

        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.PRIVATE_MESSAGE_USER_REQUEST, botRole));
    }

    private void requestContentAndSendMessage(BotRole botRole, BotRole targetRole) {
        sessionService.createSession(botRole, p -> {
            postService.sendPrivateMessageToUser(p.botRole(), targetRole.getUser().getId(), p.messages());
        });

        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.PRIVATE_MESSAGE_CONTENT_REQUEST, botRole,
                new Localizations.Service.PrivateMessageContentRequestParams(
                    entityUtil.getLocalizedTitle(botRole, targetRole),
                    targetRole.getUser().getFullName())));
    }
}
