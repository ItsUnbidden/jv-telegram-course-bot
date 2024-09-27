package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.service.course.CourseFlow;
import com.unbidden.telegramcoursesbot.util.Blockable;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * This implementation is temporary and can initiate only one course.
 * Refactoring will be needed in the future.
 */
@Component
@RequiredArgsConstructor
public class CourseCommandHandler implements CommandHandler {
    @Qualifier("howToRevampRelationshipCourse")
    private final CourseFlow course;

    @Override
    @Blockable
    public void handle(@NonNull Message message, @NonNull String[] commandParts) {
        course.initMessage(message.getFrom());
    }

    @Override
    @NonNull
    public String getCommand() {
        return "/course";
    }
}
