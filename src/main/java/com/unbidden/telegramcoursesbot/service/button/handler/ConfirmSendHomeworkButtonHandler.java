package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class ConfirmSendHomeworkButtonHandler implements ButtonHandler {
    private final HomeworkService homeworkService;

    @Override
    public void handle(String[] params, User user) {
        homeworkService.commit(Long.parseLong(params[0]));
    }
}
