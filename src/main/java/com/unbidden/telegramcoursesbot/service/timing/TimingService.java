package com.unbidden.telegramcoursesbot.service.timing;

import com.unbidden.telegramcoursesbot.model.BanTrigger;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkTrigger;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.LessonTrigger;
import com.unbidden.telegramcoursesbot.model.TimedTrigger;
import com.unbidden.telegramcoursesbot.repository.BanTriggersRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkTriggersRepository;
import com.unbidden.telegramcoursesbot.repository.LessonTriggersRepository;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class TimingService {
    private static final Logger LOGGER = LogManager.getLogger(TimingService.class);

    private final LessonTriggersRepository lessonTriggersRepository;

    private final HomeworkTriggersRepository homeworkTriggersRepository;

    private final BanTriggersRepository banTriggersRepository;

    private final EntityUtil entityUtil;

    @Transactional
    public Optional<LessonTrigger> createLessonTriggerIfNeeded(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        
        final CourseProgress progress = entityUtil.getCourseProgressForUser(botRole, courseId);
        final Lesson lesson = entityUtil.getLessonByPositionAndCourseId(botRole, progress.getStage(), courseId);

        if (lesson.getDelay() > 0 && progress.getNumberOfTimesCompleted() == 0) {
            LOGGER.debug("Lesson " + lesson.getId() + " has a delay of " + lesson.getDelay()
                    + " minutes. Creating a new lesson timed trigger...");
            final LessonTrigger trigger = new LessonTrigger();

            trigger.setProgress(progress);
            trigger.setBotRole(botRole);
            trigger.setCreatedAt(LocalDateTime.now());
            trigger.setTarget(LocalDateTime.now().plusMinutes(lesson.getDelay()));

            lessonTriggersRepository.save(trigger);

            LOGGER.debug("New trigger " + trigger.getId() + " for user " + botRole.getUser().getId()
                    + " and lesson " + lesson.getId() + " has been created. It will activate at "
                    + trigger.getTarget() + ".");
            
            return Optional.of(trigger);
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<HomeworkTrigger> createHomeworkTriggerIfNeeded(BotRole botRole, Long homeworkId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final Homework homework = entityUtil.getHomeworkById(botRole, homeworkId);
        final CourseProgress progress = entityUtil.getCourseProgressForUser(botRole, homework.getLesson().getCourse().getId());

        if (homework.getDelay() > 0 && progress.getNumberOfTimesCompleted() < 1) {
            LOGGER.debug("Homework " + homework.getId() + " has a delay of "
                    + homework.getDelay() + " minutes. Creating homework trigger...");
            final HomeworkTrigger trigger = new HomeworkTrigger();

            trigger.setProgress(entityUtil.getHomeworkProgressByHomeworkId(botRole, homeworkId));
            trigger.setBotRole(botRole);
            trigger.setCreatedAt(LocalDateTime.now());
            trigger.setTarget(LocalDateTime.now().plusMinutes(homework.getDelay()));

            homeworkTriggersRepository.save(trigger);

            LOGGER.debug("New trigger " + trigger.getId() + " for user " + botRole.getUser().getId()
                    + " and homework " + homework.getId() + " has been created. It will "
                    + "activate at " + trigger.getTarget() + ".");
            return Optional.of(trigger);
        }
        return Optional.empty();
    }

    @Transactional
    public BanTrigger createBanTrigger(BotRole botRole, int hours, boolean isGeneral) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.state(hours > 0, "hours must be greater than 0");

        LOGGER.debug("Creating timed trigger for user " + botRole.getUser().getId() + "'s ban in bot " + botRole.getBot().getId() + "...");
        final BanTrigger trigger = new BanTrigger();

        trigger.setBotRole(botRole);
        trigger.setCreatedAt(LocalDateTime.now());
        trigger.setGeneral(isGeneral);
        trigger.setTarget(LocalDateTime.now().plusHours(hours));

        LOGGER.debug("New trigger's target will be " + trigger.getTarget() + ".");
        banTriggersRepository.save(trigger);

        return trigger;
    }

    @Transactional(readOnly = true)
    public Optional<LessonTrigger> findLessonTrigger(Long botRoleId, Long courseId, int stage) {
        return lessonTriggersRepository.findByBotRoleIdAndProgressCourseIdAndProgressStage(botRoleId, courseId, stage);
    }

    @Transactional(readOnly = true)
    public boolean existsHomeworkTrigger(Long botRoleId, Long homeworkId) {
        return homeworkTriggersRepository.existsByBotRoleIdAndProgressHomeworkId(botRoleId, homeworkId);
    }

    @Transactional
    public int removeBanTriggerIfPresent(Long botRoleId) {
        return banTriggersRepository.deleteByBotRoleId(botRoleId);
    }

    @Transactional
    public void removeGeneralBanTriggerIfPresent(Long userId) {
        banTriggersRepository.deleteByBotRoleUserIdAndIsGeneralTrue(userId);
    }

    /**
     * Returns the amount of time left until this {@link TimedTrigger} will activate.
     * Calculated in hours, where 0 means less then an hour.
     */
    
    public int getTimeLeft(TimedTrigger trigger) {
        return (int)trigger.getTarget().until(LocalDateTime.now(), ChronoUnit.HOURS);
    }

    @Transactional
    public List<LessonTrigger> findAndRemoveExpiredLessonTriggers() {
        final List<LessonTrigger> expiredLessonTriggers =
                lessonTriggersRepository.findAllExpired(LocalDateTime.now());
        final List<LessonTrigger> executableTriggers = new ArrayList<>();

        for (final LessonTrigger trigger : expiredLessonTriggers) {
           LOGGER.trace("Lesson trigger " + trigger.getId() + " has expired.");
            if (trigger.getProgress().getCourse().isUnderMaintenance()) {
                LOGGER.warn("Lesson trigger " + trigger.getId() + " cannot be activated "
                        + "because course " + trigger.getProgress().getCourse().getId()
                        + " is under maintenance.");
                continue;
            }
            lessonTriggersRepository.delete(trigger);
            executableTriggers.add(trigger);
        }
        return executableTriggers;
    }

    @Transactional
    public List<HomeworkTrigger> findAndRemoveExpiredHomeworkTriggers() {
        final List<HomeworkTrigger> expiredHomeworkTriggers =
                homeworkTriggersRepository.findAllExpired(LocalDateTime.now());
        final List<HomeworkTrigger> executableTriggers = new ArrayList<>();

        for (final HomeworkTrigger trigger : expiredHomeworkTriggers) {
           LOGGER.trace("Homework trigger " + trigger.getId() + " has expired.");
           final Course course = trigger.getProgress().getHomework().getLesson().getCourse();

            if (course.isUnderMaintenance()) {
                LOGGER.warn("Homework trigger " + trigger.getId() + " cannot be activated "
                        + "because course " + course.getId() + " is under maintenance.");
                continue;
            }
            homeworkTriggersRepository.delete(trigger);
            executableTriggers.add(trigger);
        }
        return executableTriggers;
    }

    @Transactional
    public List<BanTrigger> findAndRemoveExpiredBanTriggers() {
        final List<BanTrigger> expiredBanTriggers = banTriggersRepository
                .findAllExpired(LocalDateTime.now());

        for (final BanTrigger trigger : expiredBanTriggers) {
            LOGGER.trace("Ban trigger " + trigger.getId() + " has expired.");
            banTriggersRepository.delete(trigger);
        }

        return expiredBanTriggers;
    }
}
