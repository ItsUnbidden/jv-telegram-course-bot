package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.post.PostService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostButtonHandler extends AbstractButtonHandler {
    private static final String ROLE_TYPE_PARAM = "terminal";

    private final PostService postService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.POST)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final RoleType roleType = params.get(ROLE_TYPE_PARAM) != null ? RoleType.valueOf(params.get(ROLE_TYPE_PARAM)) : null;

        if (roleType == null) {
            sessionService.createSession(user, bot, p -> {
                postService.sendMessages(p.user(), p.bot(), p.messages());
            });

            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .localize(Localizations.Service.POST_CONTENT_AND_ROLES_REQUEST, user));
        } else {
            sessionService.createSession(user, bot, p -> {
                postService.sendMessages(p.user(), p.bot(), roleType, p.messages());
            });

            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .localize(Localizations.Service.POST_CONTENT_REQUEST, user));
        }
    }
}
