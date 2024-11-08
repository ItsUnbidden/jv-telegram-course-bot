package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListAdminsButtonHandler implements ButtonHandler {
    private static final String PARAM_ADMIN_LIST = "${adminList}";

    private static final String SERVICE_GET_ADMIN_LIST = "service_get_admin_list";

    private final LocalizationLoader localizationLoader;
    
    private final UserService userService;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        if (userService.isAdmin(user)) {
            final List<UserEntity> adminList = userService.getAdminList();
            final StringBuilder builder = new StringBuilder();
    
            for (UserEntity userEntity : adminList) {
                builder.append(userEntity.getId()).append(' ').append(userEntity.getFirstName())
                        .append(' ').append((userEntity.getLastName() != null)
                        ? userEntity.getLastName() : "No last name").append(' ')
                        .append((userEntity.getUsername() != null) ? userEntity.getUsername()
                        : "No username").append(' ').append(userEntity.getLanguageCode())
                        .append('\n');
            }
            final Localization localization = localizationLoader.getLocalizationForUser(
                    SERVICE_GET_ADMIN_LIST, user, PARAM_ADMIN_LIST, builder.toString());
    
            client.sendMessage(user, localization);
        }
    }
}
