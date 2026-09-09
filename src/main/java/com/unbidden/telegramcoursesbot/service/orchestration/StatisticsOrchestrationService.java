package com.unbidden.telegramcoursesbot.service.orchestration;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.menu.multipage.supplier.BotUsersDataSupplier;
import com.unbidden.telegramcoursesbot.menu.multipage.supplier.CourseCompletedUsersDataSupplier;
import com.unbidden.telegramcoursesbot.menu.multipage.supplier.CourseStageUsersDataSupplier;
import com.unbidden.telegramcoursesbot.menu.multipage.supplier.CourseUsersDataSupplier;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.service.statistics.StatisticsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticsOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(StatisticsOrchestrationService.class);

    private static final String COURSE_ID_PARAM = "courseId";
    private static final String STAGE_PARAM = "stage";

    private final BotUsersDataSupplier botUsersDataSupplier;
    private final CourseUsersDataSupplier courseUsersDataSupplier;
    private final CourseCompletedUsersDataSupplier courseCompletedUsersDataSupplier;
    private final CourseStageUsersDataSupplier courseStageUsersDataSupplier;

    private final StatisticsService statisticsService;

    private final MenuOrchestrationService menuService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    public void sendBotStatistics(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");
        
        final var params = statisticsService.getBotStatistics(botRole.getBot());

        LOGGER.debug("All data fetched for statistics report on bot " + botRole.getBot().getId()
                + " for user " + botRole.getUser().getId() + ". Sending...");
        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.BOT_STATISTICS_REPORT, botRole, params));
        LOGGER.debug("Report sent.");
    }

    public void sendBotUsers(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        menuService.initiateMultipageList(botRole, botUsersDataSupplier);
    }

    public void sendCourseStatistics(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final var params = statisticsService.getCourseStatistics(botRole, courseId);

        LOGGER.debug("All data fetched for statistics report on course " + courseId
                + " for user " + botRole.getUser().getId() + ". Sending...");
        clientManager.sendMessage(botRole, localizationLoader
                .localize(Localizations.Service.COURSE_STATISTICS_REPORT, botRole, params));
        LOGGER.debug("Report sent.");
    }

    public void sendCourseUsers(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        menuService.initiateMultipageList(botRole, courseUsersDataSupplier, COURSE_ID_PARAM, courseId.toString());
    }

    public void sendCourseCompletedUsers(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        menuService.initiateMultipageList(botRole, courseCompletedUsersDataSupplier, COURSE_ID_PARAM, courseId.toString());
    }

    public void sendCourseStageUsers(BotRole botRole, Long courseId, int stage) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        menuService.initiateMultipageList(botRole, courseStageUsersDataSupplier,
            Map.of(
                COURSE_ID_PARAM, courseId.toString(),
                STAGE_PARAM, String.valueOf(stage)
            )
        );
    }
}
