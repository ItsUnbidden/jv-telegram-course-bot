package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListAdminsButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(ListAdminsButtonHandler.class);

    private static final String PARAM_MENTORS_INFO = "${mentorsInfo}";
    private static final String PARAM_SUPPORT_INFO = "${supportInfo}";
    private static final String PARAM_CREATOR_INFO = "${creatorInfo}";

    private static final String SERVICE_GET_ADMIN_LIST = "service_get_admin_list";

    private static final String ERROR_NO_MENTORS = "error_no_mentors";
    private static final String ERROR_NO_SUPPORT_STAFF = "error_no_support_staff";

    private final LocalizationLoader localizationLoader;
    
    private final UserService userService;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.ROLE_SETTINGS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        LOGGER.debug("Generating admins list for user " + user.getId()
                + " for bot " + bot.getId() + "...");

        final List<UserEntity> support = userService.getSupport(bot);
        final List<UserEntity> mentors = userService.getMentors(bot);
        final UserEntity creator = userService.getCreator(bot);
        final StringBuilder builder = new StringBuilder();
    
        final String creatorStr = builder.append(creator.getId()).append(' ')
                .append(creator.getFullName()).append(' ')
                .append(creator.getLanguageCode()).toString();
        builder.delete(0, builder.length());

        for (UserEntity supportUser : support) {
            builder.append(supportUser.getId()).append(' ').append(supportUser.getFullName())
                    .append(' ').append(supportUser.getLanguageCode()).append('\n');
        }
        final String supportStr;
        if (builder.length() != 0) {
            supportStr = builder.delete(builder.length() - 1, builder.length())
                    .toString();
            builder.delete(0, builder.length());
        } else {
            supportStr = localizationLoader.getLocalizationForUser(ERROR_NO_SUPPORT_STAFF, user)
                    .getData();
        }

        for (UserEntity mentor : mentors) {
            builder.append(mentor.getId()).append(' ').append(mentor.getFullName())
                    .append(' ').append(mentor.getLanguageCode()).append('\n');
        }
        final String mentorsStr;
        if (builder.length() != 0) {
            mentorsStr = builder.delete(builder.length() - 1, builder.length())
                .toString();
            builder.delete(0, builder.length());
        } else {
            mentorsStr = localizationLoader.getLocalizationForUser(ERROR_NO_MENTORS, user)
                    .getData();
        }
        builder.delete(0, builder.length());

        LOGGER.debug("List of amdins has been generated. Sending...");
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_CREATOR_INFO, creatorStr);
        parameterMap.put(PARAM_SUPPORT_INFO, supportStr);
        parameterMap.put(PARAM_MENTORS_INFO, mentorsStr);

        final Localization localization = localizationLoader.getLocalizationForUser(
                SERVICE_GET_ADMIN_LIST, user, parameterMap);

        clientManager.getClient(bot).sendMessage(user, localization);
        LOGGER.debug("Message sent.");
    }
}
