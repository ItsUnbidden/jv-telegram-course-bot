package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.StatisticsOrchestrationService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatisticsButtonHandler extends AbstractButtonHandler {
    private static final String TYPE_PARAM = "terminal";
    private static final String COURSE_STAGE_PARAM = "courseStage";
    private static final String COURSE_ID_PARAM = "courseId";

    private static final String COURSE_ALL_USERS = "courseAllUsers";
    private static final String COURSE_COMPLETED_USERS = "courseCompletedUsers";
    private static final String COURSE_GENERAL = "courseGeneral";
    private static final String BOT_USERS = "botUsers";
    private static final String BOT_GENERAL = "botGeneral";

    private final StatisticsOrchestrationService statisticsService;

    @Override
    @Security(authorities = AuthorityType.STATISTICS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final String type = params.get(TYPE_PARAM);

        switch (type) {
            case BOT_GENERAL:
                statisticsService.sendBotStatistics(user, bot);
                break;
            case BOT_USERS:
                statisticsService.sendBotUsers(user, bot);
                break;
            case COURSE_GENERAL:
                statisticsService.sendCourseStatistics(user, bot, Long.parseLong(params.get(COURSE_ID_PARAM)));
                break;
            case COURSE_ALL_USERS:
                statisticsService.sendCourseUsers(user, bot, Long.parseLong(params.get(COURSE_ID_PARAM)));
                break;
            case COURSE_COMPLETED_USERS:
                statisticsService.sendCourseCompletedUsers(user, bot, Long.parseLong(params.get(COURSE_ID_PARAM)));
                break;
            default:
                statisticsService.sendCourseStageUsers(user, bot, Long.parseLong(params.get(COURSE_ID_PARAM)), Integer.parseInt(params.get(COURSE_STAGE_PARAM)));
        }
    }
}
