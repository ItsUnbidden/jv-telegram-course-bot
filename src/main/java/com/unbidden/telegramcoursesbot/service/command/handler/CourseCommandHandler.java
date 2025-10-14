package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class CourseCommandHandler implements CommandHandler {
    private static final String COURSES_MENU = "m_crs";
    private static final String MY_COURSES_MENU = "m_myCrs";
    private static final String AVAILABLE_COURSES_MENU = "m_aCrs";

    private static final String COMMAND = "/courses";

    private final MenuService menuService;

    private final CourseService courseService;

    @Override
    @Security(authorities = {AuthorityType.BUY, AuthorityType.LAUNCH_COURSE,
            AuthorityType.LEAVE_REVIEW, AuthorityType.REFUND})
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull Message message,
            @NonNull String[] commandParts) {
        final List<String> allCoursesNamesOwnedByUser = courseService.getAllOwnedByUser(user, bot)
                .stream().map(c -> c.getName()).toList();

        if (allCoursesNamesOwnedByUser.isEmpty()) {
            menuService.initiateMenu(AVAILABLE_COURSES_MENU, user, bot);
            return;
        }

        final List<Course> availableCourses = courseService.getByBot(bot).stream()
                .filter(c -> !allCoursesNamesOwnedByUser.contains(c.getName())).toList();

        if (availableCourses.isEmpty()) {
            menuService.initiateMenu(MY_COURSES_MENU, user, bot);
            return;
        }
        menuService.initiateMenu(COURSES_MENU, user, bot);
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND;
    }

    @Override
    @NonNull
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.BUY, AuthorityType.LAUNCH_COURSE,
                AuthorityType.LEAVE_REVIEW, AuthorityType.REFUND);
    }
}
