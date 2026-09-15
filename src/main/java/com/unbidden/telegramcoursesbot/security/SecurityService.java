package com.unbidden.telegramcoursesbot.security;

import com.unbidden.telegramcoursesbot.exception.AccessDeniedException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error.AccessDeniedParams;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;
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

    private final LocalizationLoader localizationLoader;

    private final EntityUtil entityUtil;

    public boolean grantAccess(BotRole botRole, boolean isBotLordOnly, List<AuthorityType> authorities) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(authorities, "authorities cannot be null");

        LOGGER.trace("Security service is checking user " + botRole.getUser().getId()
                + "'s access in bot " + botRole.getBot().getId() + "...");
        
        if (isBotLordOnly) {
            entityUtil.checkBotLord(botRole);
        }
        try {
            if (botRole.getRole().getType() == RoleType.BANNED) {
                throw new AccessDeniedException("User " + botRole.getUser().getId() + " is banned in bot "
                        + botRole.getBot().getId(), localizationLoader.localize(
                        Error.USER_IS_BANNED_IN_BOT, botRole));
            }
            final List<AuthorityType> localAuthorities = new ArrayList<>(authorities);
            
            localAuthorities.removeAll(botRole.getRole().getAuthorities().stream().map(a -> a.getType()).toList());
            if (localAuthorities.size() > 0) {
                throw new AccessDeniedException("User " + botRole.getUser().getId() + " does not have "
                        + "required authority in bot " + botRole.getBot().getId() + ". Missing authorities: "
                        + localAuthorities + ".", localizationLoader.localize(Error.ACCESS_DENIED, botRole,
                            new AccessDeniedParams(localAuthorities)));
            }
        } catch (AccessDeniedException e) {
            return false;
        }
        LOGGER.trace("Access granted to user " + botRole.getUser().getId() + " in bot " + botRole.getBot().getId() + ".");
        return true;
    }

    public boolean grantAccess(BotRole botRole, AuthorityType... authorityTypes) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(authorityTypes, "authorityTypes cannot be null");

        return grantAccess(botRole, false, List.of(authorityTypes));
    }
}
