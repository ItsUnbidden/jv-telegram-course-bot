package com.unbidden.telegramcoursesbot.service.orchestration;

import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.repository.UserRepository;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.service.statistics.StatisticsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticsOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(StatisticsOrchestrationService.class);

    private final StatisticsService statisticsService;

    private final MenuOrchestrationService menuService;

    private final ContentOrchestrationService contentService;

    private final UserRepository userRepository;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

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

        menuService.initiateMultipageList(botRole,
            p -> {
                return localizationLoader.localize(Localizations.Service.BOT_USERS, botRole,
                        new Localizations.Service.BotUsersParams(p.currentPage(), p.numberOfPages(), p.numberOfElements(), p.data()));
            },
            (p, q) -> userRepository.findByRoleType(botRole.getBot().getId(), RoleType.USER, PageRequest.of(p, q)).map(u -> u.getFullUserInfo())
        );
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

        menuService.initiateMultipageList(botRole,
            p -> {
                return localizationLoader.localize(Localizations.Service.COURSE_USERS,
                    botRole, new Localizations.Service.CourseUsersParams(p.currentPage(), p.numberOfPages(), p.numberOfElements(), p.data(),
                    contentService.getLocalizedText(botRole, entityUtil.getCourseTitle(botRole, courseId))));
            },
            (p, q) -> userRepository.findByActiveCourseOwnership(courseId, PageRequest.of(p, q)).map(u -> u.getFullUserInfo())
        );
    }

    public void sendCourseCompletedUsers(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        menuService.initiateMultipageList(botRole,
            p -> {
                return localizationLoader.localize(Localizations.Service.COURSE_COMPLETED_USERS, botRole,
                    new Localizations.Service.CourseCompletedUsersParams(
                        p.currentPage(),
                        p.numberOfPages(),
                        p.numberOfElements(),
                        p.data(),
                        contentService.getLocalizedText(botRole, entityUtil.getCourseTitle(botRole, courseId))
                    )
                );
            },
            (p, q) -> userRepository.findAllCompletedCourse(courseId, PageRequest.of(p, q)).map(u -> u.getFullUserInfo())
        );
    }

    public void sendCourseStageUsers(BotRole botRole, Long courseId, int stage) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        menuService.initiateMultipageList(botRole,
            p -> {
                return localizationLoader.localize(Localizations.Service.COURSE_STAGE_USERS, botRole,
                    new Localizations.Service.CourseStageUsersParams(
                        p.currentPage(),
                        p.numberOfPages(),
                        p.numberOfElements(),
                        p.data(), 
                        contentService.getLocalizedText(botRole, entityUtil.getCourseTitle(botRole, courseId)),
                        stage
                    )
                );
            },
            (p, q) -> userRepository.findAllAtCourseStage(courseId, stage, PageRequest.of(p, q)).map(u -> u.getFullUserInfo())
        );
    }
}
