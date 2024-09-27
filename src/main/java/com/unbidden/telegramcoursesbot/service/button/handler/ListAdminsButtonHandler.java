package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class ListAdminsButtonHandler implements ButtonHandler {
    private final TelegramBot bot;

    private final LocalizationLoader localizationLoader;

    private final UserService userService;

    @Override
    public void handle(String[] params, User user) {
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
                    "service_get_admin_list", user, "${adminList}", builder.toString());
    
            bot.sendMessage(SendMessage.builder()
                    .chatId(user.getId())
                    .text(localization.getData())
                    .entities(localization.getEntities())
                    .build());
        }
    }
}
