package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfirmSendHomeworkButtonHandler implements ButtonHandler {
    private final HomeworkService homeworkService;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        homeworkService.commit(Long.parseLong(params[0]));
    }
}
