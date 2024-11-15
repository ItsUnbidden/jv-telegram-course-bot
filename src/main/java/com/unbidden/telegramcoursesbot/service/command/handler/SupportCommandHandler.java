package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class SupportCommandHandler implements CommandHandler {
    private static final String COMMAND = "/support";

    private static final String SUPPORT_REQUEST_MENU = "m_sr";

    private static final String ERROR_SUPPORT_STAFF_REQUEST = "error_support_staff_request";
    
    private final UserService userService;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    @Override
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        final UserEntity user = userService.getUser(message.getFrom().getId());
        final Set<UserEntity> uneligibleUsers = new HashSet<>();
        uneligibleUsers.addAll(userService.getSupport());
        uneligibleUsers.add(userService.getDiretor());
        uneligibleUsers.add(userService.getCreator());
        
        if (uneligibleUsers.contains(user)) {
            throw new ForbiddenOperationException("User " + user.getId() + " is a part of the "
                    + "staff, they are uneligible for support", localizationLoader
                    .getLocalizationForUser(ERROR_SUPPORT_STAFF_REQUEST, user));
        }
        menuService.initiateMenu(SUPPORT_REQUEST_MENU, user);
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public boolean isAdminCommand() {
        return false;
    }
}
