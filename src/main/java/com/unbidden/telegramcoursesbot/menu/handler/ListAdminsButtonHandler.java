package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListAdminsButtonHandler extends AbstractButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(ListAdminsButtonHandler.class);

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    @Override
    @Security(authorities = AuthorityType.ROLE_SETTINGS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        LOGGER.debug("Generating admins list for user " + user.getId() + " in bot " + bot.getId() + "...");

        final List<UserEntity> support = entityUtil.getSupport(bot);
        final List<UserEntity> mentors = entityUtil.getMentors(bot);
        final UserEntity creator = entityUtil.getCreator(bot);
        final StringBuilder builder = new StringBuilder();
    
        final String creatorStr = builder.append(creator.getId()).append(' ')
                .append(creator.getFullName()).append(' ')
                .append(creator.getLanguageCode()).toString();
        builder.delete(0, builder.length());

        for (final UserEntity supportUser : support) {
            builder.append(supportUser.getId()).append(' ').append(supportUser.getFullName())
                    .append(' ').append(supportUser.getLanguageCode()).append('\n');
        }
        final String supportStr;
        if (builder.length() != 0) {
            supportStr = builder.delete(builder.length() - 1, builder.length())
                    .toString();
            builder.delete(0, builder.length());
        } else {
            supportStr = localizationLoader.localize(Localizations.Error.NO_SUPPORT_STAFF, user).getData();
        }

        for (final UserEntity mentor : mentors) {
            builder.append(mentor.getId()).append(' ').append(mentor.getFullName())
                    .append(' ').append(mentor.getLanguageCode()).append('\n');
        }
        final String mentorsStr;
        if (builder.length() != 0) {
            mentorsStr = builder.delete(builder.length() - 1, builder.length())
                .toString();
            builder.delete(0, builder.length());
        } else {
            mentorsStr = localizationLoader.localize(Localizations.Error.NO_MENTORS, user).getData();
        }
        builder.delete(0, builder.length());

        LOGGER.debug("List of admins has been generated. Sending...");

        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.GET_ADMIN_LIST, user, new Localizations.Service.GetAdminListParams(mentorsStr, supportStr, creatorStr)));
        LOGGER.debug("Message sent.");
    }
}
