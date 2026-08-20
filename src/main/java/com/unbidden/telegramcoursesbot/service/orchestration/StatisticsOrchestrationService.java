package com.unbidden.telegramcoursesbot.service.orchestration;

import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.repository.UserRepository;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
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

    public void sendBotStatistics(UserEntity user, Bot bot) {
        final var params = statisticsService.getBotStatistics(bot);

        LOGGER.debug("All data fetched for statistics report on bot " + bot.getId()
                + " for user " + user.getId() + ". Sending...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.localize(
                Localizations.Service.BOT_STATISTICS_REPORT, user, params));
        LOGGER.debug("Report sent.");
    }

    public void sendBotUsers(UserEntity user, Bot bot) {
        menuService.initiateMultipageList(user, bot,
            p -> {
                return localizationLoader.localize(Localizations.Service.BOT_USERS, user,
                        new Localizations.Service.BotUsersParams(p.currentPage(), p.numberOfPages(), p.numberOfElements(), p.data()));
            },
            (p, q) -> userRepository.findByRoleType(bot.getId(), RoleType.USER, PageRequest.of(p, q)).map(u -> u.getFullUserInfo())
        );
    }

    public void sendCourseStatistics(UserEntity user, Bot bot, Long courseId) {
        final var params = statisticsService.getCourseStatistics(user, bot, courseId);

        LOGGER.debug("All data fetched for statistics report on course " + courseId
                + " for user " + user.getId() + ". Sending...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .localize(Localizations.Service.COURSE_STATISTICS_REPORT, user, params));
        LOGGER.debug("Report sent.");
    }

    public void sendCourseUsers(UserEntity user, Bot bot, Long courseId) {
        menuService.initiateMultipageList(user, bot,
            p -> {
                return localizationLoader.localize(Localizations.Service.COURSE_USERS,
                    user, new Localizations.Service.CourseUsersParams(p.currentPage(), p.numberOfPages(), p.numberOfElements(), p.data(),
                    contentService.getLocalizedText(user, bot, entityUtil.getCourseTitle(user, bot, courseId))));
            },
            (p, q) -> userRepository.findByActiveCourseOwnership(courseId, PageRequest.of(p, q)).map(u -> u.getFullUserInfo())
        );
    }

    public void sendCourseCompletedUsers(UserEntity user, Bot bot, Long courseId) {
        menuService.initiateMultipageList(user, bot,
            p -> {
                return localizationLoader.localize(
                    Localizations.Service.COURSE_COMPLETED_USERS, user,
                        new Localizations.Service.CourseCompletedUsersParams(p.currentPage(), p.numberOfPages(),
                        p.numberOfElements(), p.data(), contentService.getLocalizedText(user, bot,
                        entityUtil.getCourseTitle(user, bot, courseId))));
            },
            (p, q) -> userRepository.findAllCompletedCourse(courseId, PageRequest.of(p, q)).map(u -> u.getFullUserInfo())
        );
    }

    public void sendCourseStageUsers(UserEntity user, Bot bot, Long courseId, int stage) {
        menuService.initiateMultipageList(user, bot,
            p -> {
                return localizationLoader.localize(Localizations.Service.COURSE_STAGE_USERS, user,
                    new Localizations.Service.CourseStageUsersParams(p.currentPage(), p.numberOfPages(),
                    p.numberOfElements(), p.data(), contentService.getLocalizedText(user, bot,
                        entityUtil.getCourseTitle(user, bot, courseId)), stage));
            },
            (p, q) -> userRepository.findAllAtCourseStage(courseId, stage, PageRequest.of(p, q)).map(u -> u.getFullUserInfo())
        );
    }
}
