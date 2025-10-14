package com.unbidden.telegramcoursesbot.service.statistics;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.repository.CourseProgressRepository;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.repository.PaymentDetailsRepository;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private static final Logger LOGGER = LogManager.getLogger(StatisticsService.class); 

    private static final String SERVICE_BOT_STATISTICS_REPORT = "service_bot_statistics_report";
    private static final String SERVICE_COURSE_STATISTICS_REPORT =
            "service_course_statistics_report";
    private static final String SERVICE_COURSE_STAGE_USERS = "service_course_stage_users";
    private static final String SERVICE_COURSE_COMPLETED_USERS = "service_course_completed_users";
    private static final String SERVICE_COURSE_USERS = "service_course_users";
    private static final String SERVICE_BOT_USERS = "service_bot_users";

    private static final String PARAM_NUMBER_OF_BANNED_USERS = "${numberOfBannedUsers}";
    private static final String PARAM_NUMBER_OF_USERS = "${numberOfUsers}";
    private static final String PARAM_TOTAL_STARS_INCOME = "${totalStarsIncome}";
    private static final String PARAM_COURSES_REFUNDED = "${coursesRefunded}";
    private static final String PARAM_COURSES_BOUGHT = "${coursesBought}";
    private static final String PARAM_NUMBER_OF_COURSES = "${numberOfCourses}";
    private static final String PARAM_BOTNAME = "${botname}";
    private static final String PARAM_COURSES_CURRENTLY_OWNED = "${coursesCurrentlyOwned}";
    private static final String PARAM_COURSES_TAKEN = "${coursesTaken}";
    private static final String PARAM_COURSES_GIFTED = "${coursesGifted}";
    private static final String PARAM_COURSE_NAME = "${courseName}";
    private static final String PARAM_NUMBER_OF_USERS_WHO_COMPLETED =
            "${numberOfUsersWhoCompleted}";
    private static final String PARAM_NUMBER_OF_OWNERS = "${numberOfOwners}";
    private static final String PARAM_TIMES_TAKEN = "${timesTaken}";
    private static final String PARAM_TIMES_GIFTED = "${timesGifted}";
    private static final String PARAM_TIMES_REFUNDED = "${timesRefunded}";
    private static final String PARAM_TIMES_BOUGHT = "${timesBought}";
    private static final String PARAM_STAGE = "${stage}";

    private static final String COURSE_NAME = "course_%s_name";

    private final PaymentDetailsRepository paymentDetailsRepository;

    private final CourseRepository courseRepository;

    private final BotRoleRepository botRoleRepository;
    
    private final CourseProgressRepository courseProgressRepository;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    public void sendBotStatistics(@NonNull UserEntity user, @NonNull Bot bot) {
        final Map<String, Object> params = new HashMap<>();

        params.put(PARAM_BOTNAME, bot.getName());
        params.put(PARAM_NUMBER_OF_COURSES, courseRepository.countByBot(bot));
        params.put(PARAM_COURSES_BOUGHT, paymentDetailsRepository
                .countByBotAndIsGiftedFalse(bot));
        params.put(PARAM_COURSES_REFUNDED, paymentDetailsRepository
                .countByBotAndRefundedAtIsNotNull(bot));
        params.put(PARAM_COURSES_GIFTED, paymentDetailsRepository.countByBotAndIsGiftedTrue(bot));
        params.put(PARAM_COURSES_TAKEN, paymentDetailsRepository
                .countByBotAndIsGiftedTrueAndIsValidFalse(bot));
        params.put(PARAM_COURSES_CURRENTLY_OWNED, paymentDetailsRepository
                .countByBotAndIsValidTrue(bot));
        params.put(PARAM_TOTAL_STARS_INCOME, paymentDetailsRepository.getTotalBotIncome(bot,
                LocalDateTime.now()));
        params.put(PARAM_NUMBER_OF_USERS, botRoleRepository.countByBotAndRoleType(bot,
                RoleType.USER));
        params.put(PARAM_NUMBER_OF_BANNED_USERS, botRoleRepository.countByBotAndRoleType(bot,
                RoleType.BANNED));

        LOGGER.debug("All data fetched for statistics report on bot " + bot.getName()
                + " for user " + user.getId() + ". Sending...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.getLocalizationForUser(
                SERVICE_BOT_STATISTICS_REPORT, user, params));
        LOGGER.debug("Report sent.");
    }

    @Override
    public void sendBotUsers(@NonNull UserEntity user, @NonNull Bot bot) {
        menuService.initiateMultipageList(user, bot,
                m -> {
                    m.put(PARAM_BOTNAME, bot.getName());

                    return localizationLoader.getLocalizationForUser(SERVICE_BOT_USERS,
                        user, m);
                },
                (p, q) -> botRoleRepository.findByBotAndRoleType(bot, RoleType.USER,
                    PageRequest.of(p, q)).stream().map(br -> br.getUser().getFullUserInfo())
                    .toList(),
                () -> botRoleRepository.countByBotAndRoleType(bot, RoleType.USER));
    }

    @Override
    public void sendCourseStatistics(@NonNull UserEntity user, @NonNull Course course) {
        final Map<String, Object> params = new HashMap<>();

        params.put(PARAM_COURSE_NAME, localizationLoader.getLocalizationForUser(
                COURSE_NAME.formatted(course.getName()), user).getData());
        params.put(PARAM_TIMES_BOUGHT, paymentDetailsRepository
                .countByCourseAndIsGiftedFalse(course));
        params.put(PARAM_TIMES_REFUNDED, paymentDetailsRepository
                .countByCourseAndRefundedAtIsNotNull(course));
        params.put(PARAM_TIMES_GIFTED, paymentDetailsRepository
                .countByCourseAndIsGiftedTrue(course));
        params.put(PARAM_TIMES_TAKEN, paymentDetailsRepository
                .countByCourseAndIsGiftedTrueAndIsValidFalse(course));
        params.put(PARAM_NUMBER_OF_OWNERS, paymentDetailsRepository
                .countByCourseAndIsValidTrue(course));
        params.put(PARAM_TOTAL_STARS_INCOME, paymentDetailsRepository
                .getTotalCourseIncome(course, LocalDateTime.now()));
        params.put(PARAM_NUMBER_OF_USERS_WHO_COMPLETED, courseProgressRepository
                .countByCourseAndNumberOfTimesCompletedGreaterThan(course, 0));

        LOGGER.debug("All data fetched for statistics report on course " + course.getName()
                + " for user " + user.getId() + ". Sending...");
        clientManager.getClient(course.getBot()).sendMessage(user, localizationLoader
                .getLocalizationForUser(SERVICE_COURSE_STATISTICS_REPORT, user, params));
        LOGGER.debug("Report sent.");
    }

    @Override
    public void sendCourseUsers(@NonNull UserEntity user, @NonNull Course course) {
        menuService.initiateMultipageList(user, course.getBot(),
                m -> {
                    m.put(PARAM_COURSE_NAME, localizationLoader.getLocalizationForUser(
                        COURSE_NAME.formatted(course.getName()), user).getData());

                    return localizationLoader.getLocalizationForUser(SERVICE_COURSE_USERS,
                        user, m);
                },
                (p, q) -> paymentDetailsRepository.findByCourseAndIsValidTrue(course,
                    PageRequest.of(p, q)).stream().map(pd -> pd.getUser().getFullUserInfo())
                    .toList(),
                () -> paymentDetailsRepository.countByCourseAndIsValidTrue(course));
    }

    @Override
    public void sendCourseCompletedUsers(@NonNull UserEntity user, @NonNull Course course) {
        menuService.initiateMultipageList(user, course.getBot(),
                m -> {
                    m.put(PARAM_COURSE_NAME, localizationLoader.getLocalizationForUser(
                        COURSE_NAME.formatted(course.getName()), user).getData());

                    return localizationLoader.getLocalizationForUser(
                        SERVICE_COURSE_COMPLETED_USERS, user, m);
                },
                (p, q) -> courseProgressRepository.findByCourseAndNumberOfTimesCompletedGreaterThan(
                    course, 0, PageRequest.of(p, q)).stream()
                    .map(cp -> cp.getUser().getFullUserInfo()).toList(),
                () -> courseProgressRepository.countByCourseAndNumberOfTimesCompletedGreaterThan(
                    course, 0));
    }

    @Override
    public void sendCourseStageUsers(@NonNull UserEntity user, @NonNull Course course,
            int stage) {
        menuService.initiateMultipageList(user, course.getBot(),
                m -> {
                    m.put(PARAM_COURSE_NAME, localizationLoader.getLocalizationForUser(
                        COURSE_NAME.formatted(course.getName()), user).getData());
                    m.put(PARAM_STAGE, stage);

                    return localizationLoader.getLocalizationForUser(SERVICE_COURSE_STAGE_USERS,
                        user, m);
                },
                (p, q) -> courseProgressRepository
                    .findByCourseAndStageAndNumberOfTimesCompleted(course, stage, 0,
                        PageRequest.of(p, q)).stream().map(cp -> cp.getUser().getFullUserInfo())
                        .toList(),
                () -> courseProgressRepository
                    .countByCourseAndStageAndNumberOfTimesCompleted(course, stage, 0));
    }
}
