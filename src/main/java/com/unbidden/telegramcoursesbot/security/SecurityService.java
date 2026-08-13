package com.unbidden.telegramcoursesbot.security;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.AccessDeniedException;
import com.unbidden.telegramcoursesbot.exception.CallbackQueryAnswerException;
import com.unbidden.telegramcoursesbot.exception.ExceptionHandlerManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error.AccessDeniedParams;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.Authority;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class SecurityService {
    private static final Logger LOGGER = LogManager.getLogger(SecurityService.class);

    private final BotRoleRepository botRoleRepository;

    private final ExceptionHandlerManager exceptionHandlerManager;

    private final MenuOrchestrationService menuService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    public boolean grantAccess(UserEntity user, Bot bot, boolean isBotLordOnly, List<Authority> authorities) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(authorities, "authorities cannot be null");

        LOGGER.trace("Security service is checking user " + user.getId()
                + "'s access in bot " + bot.getId() + "...");
        
        if (isBotLordOnly) {
            entityUtil.checkBotLord(user, bot);
        }
        try {
            final BotRole botRole = botRoleRepository.findByBotIdAndUserId(bot.getId(),
                user.getId()).orElseThrow(() -> new AccessDeniedException("User " + user.getId()
                + " has no role in bot " + bot.getId(), localizationLoader
                .localize(Error.USER_NOT_REGISTRED, user)));

            if (botRole.getRole().getType() == RoleType.BANNED) {
                throw new AccessDeniedException("User " + user.getId() + " is banned in bot "
                        + bot.getId(), localizationLoader.localize(
                        Error.USER_IS_BANNED_IN_BOT, user));
            }
            final List<Authority> localAuthorities = new ArrayList<>(authorities);
            
            localAuthorities.removeAll(botRole.getRole().getAuthorities());
            if (localAuthorities.size() > 0) {
                final List<AuthorityType> missingAuthorityTypes = localAuthorities.stream()
                        .map(a -> a.getType()).toList();

                throw new AccessDeniedException("User " + user.getId() + " does not have "
                        + "required authority in bot " + bot.getId() + ". Missing authorities: "
                        + missingAuthorityTypes + ".",
                        localizationLoader.localize(Error.ACCESS_DENIED, user,
                            new AccessDeniedParams(missingAuthorityTypes)));
            }
        } catch (AccessDeniedException e) {
            LOGGER.debug("Access to bot " + bot.getId() + " denied for user " + user.getId());
            try {
                menuService.answerPotentialCallbackQuery(user, bot);
            } catch (CallbackQueryAnswerException e1) {
                LOGGER.error("Unable to answer callback query", e1);
                clientManager.getClient(bot).sendMessage(exceptionHandlerManager
                        .handleException(entityUtil.getDiretor(), bot, e1));
            }
            clientManager.getClient(bot).sendMessage(exceptionHandlerManager
                    .handleException(user, bot, e));
            return false;
        }
        LOGGER.trace("Access granted to user " + user.getId() + " in bot " + bot.getId() + ".");
        return true;
    }

    public boolean grantAccess(UserEntity user, Bot bot, AuthorityType... authorityTypes) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(authorityTypes, "authorityTypes cannot be null");

        final List<Authority> authorities = entityUtil.parseAuthorities(List.of(authorityTypes));

        return grantAccess(user, bot, false, authorities);
    }
}
