package com.unbidden.telegramcoursesbot.service.menu.handler;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CourseNextStageButtonHandler implements ButtonHandler {
    private final CourseOrchestrationService courseService;

    @Override
    public void handle(UserEntity user, Bot bot, String[] params) {
        courseService.next(user, bot, Long.parseLong(params[1]), Long.parseLong(params[0]));
    }
}
