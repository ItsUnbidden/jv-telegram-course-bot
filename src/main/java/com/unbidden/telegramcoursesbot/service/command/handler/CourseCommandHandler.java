package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.service.course.CourseFlow;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
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
    public void handle(Message message, String[] commandParts) {
        course.initMessage(message.getFrom());
    }

    @Override
    public String getCommand() {
        return "/course";
    }
}
