package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.Blockable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class CourseCommandHandler implements CommandHandler {
    private static final String COURSES_MENU = "m_crs";
    private static final String MY_COURSES_MENU = "m_myCrs";
    private static final String AVAILABLE_COURSES_MENU = "m_aCrs";

    private static final String COMMAND = "/courses";

    private final MenuService menuService;

    private final CourseService courseService;

    private final UserService userService;

    @Override
    @Blockable
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        final UserEntity user = userService.getUser(message.getFrom().getId());
        final List<String> allCoursesNamesOwnedByUser = courseService.getAllOwnedByUser(user)
                .stream().map(c -> c.getName()).toList();

        if (allCoursesNamesOwnedByUser.isEmpty()) {
            menuService.initiateMenu(AVAILABLE_COURSES_MENU, user);
            return;
        }

        final List<Course> availableCourses = courseService.getAll().stream()
                .filter(c -> !allCoursesNamesOwnedByUser.contains(c.getName())).toList();

        if (availableCourses.isEmpty()) {
            menuService.initiateMenu(MY_COURSES_MENU, user);
            return;
        }
        menuService.initiateMenu(COURSES_MENU, user);
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
