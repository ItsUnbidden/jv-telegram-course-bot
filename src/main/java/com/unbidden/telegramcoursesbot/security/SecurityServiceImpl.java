package com.unbidden.telegramcoursesbot.security;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.AccessDeniedException;
import com.unbidden.telegramcoursesbot.exception.CallbackQueryAnswerException;
import com.unbidden.telegramcoursesbot.exception.ExceptionHandlerManager;
import com.unbidden.telegramcoursesbot.model.Authority;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
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

    private static final String MISSING_AUTHORITIES = "{missingAuthorities}";
    
    private static final String ERROR_USER_IS_BANNED_IN_BOT = "error_user_is_banned_in_bot";
    private static final String ERROR_USER_NOT_REGISTRED = "error_user_not_registred";
    private static final String ERROR_ACCESS_DENIED = "error_access_denied";

    private final BotRoleRepository botRoleRepository;

    private final ExceptionHandlerManager exceptionHandlerManager;

    private final MenuService menuService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    public boolean grantAccess(@NonNull Bot bot, @NonNull UserEntity user,
            @NonNull List<Authority> authorities) {
        LOGGER.trace("Security service is checking user " + user.getId()
                + "'s access in bot " + bot.getName() + "...");
        try {
            final BotRole botRole = botRoleRepository.findByBotAndUser(bot,
                user).orElseThrow(() -> new AccessDeniedException("User " + user.getId()
                + " has no role in bot " + bot.getName(), localizationLoader
                .getLocalizationForUser(ERROR_USER_NOT_REGISTRED, user)));

            if (botRole.getRole().getType().equals(RoleType.BANNED)) {
                throw new AccessDeniedException("User " + user.getId() + " is banned in bot "
                        + bot.getName(), localizationLoader.getLocalizationForUser(
                        ERROR_USER_IS_BANNED_IN_BOT, user));
            }
            authorities.removeAll(botRole.getRole().getAuthorities());
            if (authorities.size() != 0) {
                final List<AuthorityType> missingAuthorityTypes = authorities.stream()
                        .map(a -> a.getType()).toList();
                throw new AccessDeniedException("User " + user.getId() + " does not have "
                        + "required authority in bot " + bot.getId() + ". Missing authorities: "
                        + authorities.stream().map(a -> a.getType()).toList() + ".",
                        localizationLoader.getLocalizationForUser(ERROR_ACCESS_DENIED, user,
                        MISSING_AUTHORITIES, missingAuthorityTypes));
            }
        } catch (AccessDeniedException e) {
            LOGGER.debug("Access to bot " + bot.getId() + " denied for user " + user.getId());
            try {
                menuService.answerPotentialCallbackQuery(user, bot);
            } catch (CallbackQueryAnswerException e1) {
                LOGGER.error("Unable to answer callback query", e);
                clientManager.getClient(bot).sendMessage(exceptionHandlerManager
                        .handleException(userService.getDiretor(), bot, e));
            }
            clientManager.getClient(bot).sendMessage(exceptionHandlerManager
                    .handleException(user, bot, e));
            return false;
        }
        LOGGER.trace("Access granted to user " + user.getId() + " in bot " + bot.getName() + ".");
        return true;
    }

    @Override
    public boolean grantAccess(@NonNull Bot bot, @NonNull UserEntity user,
            @NonNull AuthorityType... authorityTypes) {
        final List<Authority> authorities = userService.parseAuthorities(authorityTypes);

        return grantAccess(bot, user, authorities);
    }
}
