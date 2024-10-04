package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.service.course.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class TestCourseCommandHandler implements CommandHandler {
    private final CourseService courseService;

    @Override
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        courseService.initMessage(message.getFrom(), "test_course");
    }

    @Override
    @NonNull
    public String getCommand() {
        return "/testcourse";
    }

    @Override
    public boolean isAdminCommand() {
        return false;
    }
}
