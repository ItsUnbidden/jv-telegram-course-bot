package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.util.Blockable;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

// TODO: implement this class. Menu is supposed to be shown with available courses to the user
@Component
@RequiredArgsConstructor
public class CourseCommandHandler implements CommandHandler {
    private final CourseService courseService;

    @Override
    @Blockable
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        
    }

    @Override
    @NonNull
    public String getCommand() {
        return "/course";
    }

    @Override
    public boolean isAdminCommand() {
        return false;
    }
}
