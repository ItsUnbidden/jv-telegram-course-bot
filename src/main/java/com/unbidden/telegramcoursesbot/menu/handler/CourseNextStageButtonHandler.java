package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CourseNextStageButtonHandler extends AbstractButtonHandler {
    private final CourseOrchestrationService courseService;

    @Override
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        courseService.next(user, bot, Long.parseLong(params[1]), Long.parseLong(params[0]));
    }
}
