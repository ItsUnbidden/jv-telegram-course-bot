package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.statistics.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatisticsButtonHandler implements ButtonHandler {
    private static final String COURSE_COMPLETED_USERS = "ccu";
    private static final String COURSE_ALL_USERS = "cau";
    private static final String COURSE_STATISTICS = "cs";
    private static final String BOT_USERS_STATISTICS = "bus";
    private static final String GENERAL_BOT_STATISTICS = "gbs";

    private final StatisticsService statisticsService;

    private final CourseService courseService;

    @Override
    @Security(authorities = AuthorityType.STATISTICS)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        switch (params[params.length - 1]) {
            case GENERAL_BOT_STATISTICS:
                statisticsService.sendBotStatistics(user, bot);
                break;
            case BOT_USERS_STATISTICS:
                statisticsService.sendBotUsers(user, bot);
                break;
            case COURSE_STATISTICS:
                statisticsService.sendCourseStatistics(user, courseService
                        .getCourseByName(params[0], user, bot));
                break;
            case COURSE_ALL_USERS:
                statisticsService.sendCourseUsers(user, courseService
                        .getCourseByName(params[0], user, bot));
                break;
            case COURSE_COMPLETED_USERS:
                statisticsService.sendCourseCompletedUsers(user, courseService
                        .getCourseByName(params[0], user, bot));
                break;
            default:
                statisticsService.sendCourseStageUsers(user, courseService
                        .getCourseByName(params[0], user, bot), Integer.parseInt(
                        params[params.length - 1]));
        }
    }
}
