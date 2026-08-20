package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
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
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        sessionService.createSession(user, bot, p -> {
            validatorUtil.checkExactExpectedMessages(p.user(), p.messages(), 1);
            final long userId = validatorUtil.parseId(p.user(), p.messages().getFirst());
            final UserEntity target = entityUtil.getUser(userId, user.getLanguageCode());

            postService.checkUserIsInBot(user, bot, target);
            requestContentAndSendMessage(user, bot, target);
        }, true);

        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.PRIVATE_MESSAGE_USER_REQUEST, user));
    }

    private void requestContentAndSendMessage(UserEntity user, Bot bot, UserEntity target) {
        sessionService.createSession(user, bot, p -> {
            postService.sendPrivateMessageToUser(p.user(), p.bot(), target, p.messages());
        });

        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.PRIVATE_MESSAGE_CONTENT_REQUEST, user,
                new Localizations.Service.PrivateMessageContentRequestParams(
                    entityUtil.getLocalizedTitle(user, bot, target), target.getFullName())));
    }
}
