package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class CourseSettingsCommandHandler implements CommandHandler {
    private static final String COURSE_SETTINGS_MENU = "m_crsOpt";

    private static final String COMMAND = "/coursesettings";

    private final MenuService menuService;

    @Override
    @Security(authorities = {AuthorityType.COURSE_SETTINGS, AuthorityType.GIVE_COURSE})
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull Message message,
            @NonNull String[] commandParts) {
        menuService.initiateMenu(COURSE_SETTINGS_MENU, user, bot);
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND;
    }

    @Override
    @NonNull
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.COURSE_SETTINGS, AuthorityType.GIVE_COURSE);
    }
}
