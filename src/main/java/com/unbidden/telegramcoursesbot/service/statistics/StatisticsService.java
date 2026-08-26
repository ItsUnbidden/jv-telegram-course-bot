package com.unbidden.telegramcoursesbot.service.statistics;

import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.CourseOwnership.OwnershipSource;
import com.unbidden.telegramcoursesbot.model.CourseOwnership.OwnershipStatus;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.repository.CourseOwnershipRepository;
import com.unbidden.telegramcoursesbot.repository.CourseProgressRepository;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.repository.PaymentDetailsRepository;
import com.unbidden.telegramcoursesbot.repository.TelegramPaymentDetailsRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final PaymentDetailsRepository paymentDetailsRepository;

    private final TelegramPaymentDetailsRepository telegramPaymentDetailsRepository;

    private final CourseRepository courseRepository;

    private final BotRoleRepository botRoleRepository;
    
    private final CourseProgressRepository courseProgressRepository;

    private final CourseOwnershipRepository courseOwnershipRepository;

    private final ContentOrchestrationService contentService;

    private final EntityUtil entityUtil;

    @Transactional(readOnly = true)
    public Localizations.Service.BotStatisticsReportParams getBotStatistics(Bot bot) {
        return new Localizations.Service.BotStatisticsReportParams(courseRepository.countByBotId(bot.getId()),
                paymentDetailsRepository.countByBotId(bot.getId()),
                telegramPaymentDetailsRepository.countByBotIdAndRefundedAtIsNotNull(bot.getId()),
                courseOwnershipRepository.countByCourseBotIdAndStatus(bot.getId(), OwnershipStatus.ACTIVE),
                telegramPaymentDetailsRepository.getTotalStarsIncomeInBot(bot.getId()),
                courseOwnershipRepository.countByCourseBotIdAndSourceAndStatus(bot.getId(), OwnershipSource.GIFTED, OwnershipStatus.ACTIVE),
                botRoleRepository.countByBotIdAndRoleType(bot.getId(), RoleType.USER),
                botRoleRepository.countByBotIdAndRoleType(bot.getId(), RoleType.BANNED));
    }

    @Transactional(readOnly = true)
    public Localizations.Service.CourseStatisticsReportParams getCourseStatistics(UserEntity user, Bot bot, Long courseId) {
        final Course course = entityUtil.getCourseById(user, bot, courseId);

        return new Localizations.Service.CourseStatisticsReportParams(contentService.getLocalizedText(user, bot, course.getTitle().getId()),
                paymentDetailsRepository.countByCourseId(courseId),
                telegramPaymentDetailsRepository.countByCourseIdAndRefundedAtIsNotNull(courseId),
                telegramPaymentDetailsRepository.getTotalStarsIncomeForCourse(courseId),
                courseOwnershipRepository.countByCourseIdAndStatus(courseId, OwnershipStatus.ACTIVE),
                courseProgressRepository.countByCourseIdAndNumberOfTimesCompletedGreaterThan(courseId, 0),
                courseOwnershipRepository.countByCourseIdAndSourceAndStatus(courseId, OwnershipSource.GIFTED, OwnershipStatus.ACTIVE));
    }
}
