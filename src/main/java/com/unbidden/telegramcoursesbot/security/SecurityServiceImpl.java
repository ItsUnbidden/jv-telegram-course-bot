package com.unbidden.telegramcoursesbot.security;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.AccessDeniedException;
import com.unbidden.telegramcoursesbot.exception.CallbackQueryAnswerException;
import com.unbidden.telegramcoursesbot.exception.ExceptionHandlerManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error.AccessDeniedParams;
import com.unbidden.telegramcoursesbot.model.Authority;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {
    private static final Logger LOGGER = LogManager.getLogger(SecurityServiceImpl.class);

    private final BotRoleRepository botRoleRepository;

    private final ExceptionHandlerManager exceptionHandlerManager;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    @Override
    public boolean grantAccess(@NonNull Bot bot, @NonNull UserEntity user,
            @NonNull List<Authority> authorities) {
        LOGGER.trace("Security service is checking user " + user.getId()
                + "'s access in bot " + bot.getId() + "...");
        try {
            final BotRole botRole = botRoleRepository.findByBotIdAndUserId(bot.getId(),
                user.getId()).orElseThrow(() -> new AccessDeniedException("User " + user.getId()
                + " has no role in bot " + bot.getId(), localizationLoader
                .getLocalizationForUser(Error.USER_NOT_REGISTRED, user)));

            if (botRole.getRole().getType() == RoleType.BANNED) {
                throw new AccessDeniedException("User " + user.getId() + " is banned in bot "
                        + bot.getId(), localizationLoader.getLocalizationForUser(
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
                        localizationLoader.getLocalizationForUser(Error.ACCESS_DENIED, user,
                            new AccessDeniedParams(missingAuthorityTypes)));
            }
        } catch (AccessDeniedException e) {
            LOGGER.debug("Access to bot " + bot.getId() + " denied for user " + user.getId());
            try {
                menuService.answerPotentialCallbackQuery(user, bot);
            } catch (CallbackQueryAnswerException e1) {
                LOGGER.error("Unable to answer callback query", e);
                clientManager.getClient(bot).sendMessage(exceptionHandlerManager
                        .handleException(entityUtil.getDiretor(), bot, e));
            }
            clientManager.getClient(bot).sendMessage(exceptionHandlerManager
                    .handleException(user, bot, e));
            return false;
        }
        LOGGER.trace("Access granted to user " + user.getId() + " in bot " + bot.getId() + ".");
        return true;
    }

    @Override
    public boolean grantAccess(@NonNull Bot bot, @NonNull UserEntity user,
            @NonNull AuthorityType... authorityTypes) {
        final List<Authority> authorities = entityUtil.parseAuthorities(List.of(authorityTypes));

        return grantAccess(bot, user, authorities);
    }
}
