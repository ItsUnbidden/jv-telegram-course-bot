package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class NextStageButtonHandler implements ButtonHandler {
    private final CourseService courseService;

    private final UserService userService;

    @Override
    public void handle(String[] params, User user) {
        courseService.next(userService.getUser(user.getId()), params[0]);
    }
}
