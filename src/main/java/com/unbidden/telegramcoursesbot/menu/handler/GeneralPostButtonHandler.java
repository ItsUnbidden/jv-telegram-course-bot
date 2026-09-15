package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.post.PostService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeneralPostButtonHandler extends AbstractButtonHandler {
    private static final String ROLE_TYPE_PARAM = "terminal";

    private final ContentSessionService sessionService;

    private final PostService postService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = {AuthorityType.MAINTENANCE, AuthorityType.POST}, isBotLordOnly = true)
    public void handle(BotRole botRole, Map<String, String> params) {
        final RoleType roleType = params.get(ROLE_TYPE_PARAM) != null ? RoleType.valueOf(params.get(ROLE_TYPE_PARAM)) : null;

        if (roleType == null) {
            sessionService.createSession(botRole, p -> {
                postService.sendGeneralMessages(p.botRole(), p.messages());
            });

            clientManager.sendMessage(botRole, loader.localize(Localizations.Service.GENERAL_POST_CONTENT_AND_ROLES_REQUEST, botRole));
        } else {
            sessionService.createSession(botRole, p -> {
                postService.sendGeneralMessages(p.botRole(), roleType, p.messages());
            });

            clientManager.sendMessage(botRole, loader.localize(Localizations.Service.GENERAL_POST_CONTENT_REQUEST, botRole));
        }
    }
}
