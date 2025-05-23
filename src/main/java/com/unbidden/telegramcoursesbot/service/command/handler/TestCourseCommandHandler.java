package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.service.course.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class TestCourseCommandHandler implements CommandHandler {
    private static final String COMMAND = "/testcourse";

    private static final String TEST_COURSE = "test_course";
    
    private final CourseService courseService;

    @Override
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        courseService.initMessage(message.getFrom(), TEST_COURSE);
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
