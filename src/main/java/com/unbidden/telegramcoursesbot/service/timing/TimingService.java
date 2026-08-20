package com.unbidden.telegramcoursesbot.service.timing;

import com.unbidden.telegramcoursesbot.model.BanTrigger;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkTrigger;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.LessonTrigger;
import com.unbidden.telegramcoursesbot.model.TimedTrigger;
import com.unbidden.telegramcoursesbot.model.UserEntity;
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
    public Optional<LessonTrigger> createLessonTriggerIfNeeded(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        
        final CourseProgress progress = entityUtil.getCourseProgressForUser(user, bot, courseId);
        final Lesson lesson = entityUtil.getLessonByPositionAndCourseId(user, bot, progress.getStage(), courseId);

        if (lesson.getDelay() > 0 && progress.getNumberOfTimesCompleted() == 0) {
            LOGGER.debug("Lesson " + lesson.getId() + " has a delay of " + lesson.getDelay()
                    + " minutes. Creating a new lesson timed trigger...");
            final LessonTrigger trigger = new LessonTrigger();

            trigger.setProgress(progress);
            trigger.setBot(progress.getCourse().getBot());
            trigger.setUser(progress.getUser());
            trigger.setCreatedAt(LocalDateTime.now());
            trigger.setTarget(LocalDateTime.now().plusMinutes(lesson.getDelay()));

            lessonTriggersRepository.save(trigger);

            LOGGER.debug("New trigger " + trigger.getId() + " for user " + user.getId()
                    + " and lesson " + lesson.getId() + " has been created. It will activate at "
                    + trigger.getTarget() + ".");
            
            return Optional.of(trigger);
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<HomeworkTrigger> createHomeworkTriggerIfNeeded(UserEntity user, Bot bot, Long homeworkId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final Homework homework = entityUtil.getHomeworkById(user, bot, homeworkId);
        final CourseProgress progress = entityUtil.getCourseProgressById(user, bot, homework.getLesson().getCourse().getId());

        if (homework.getDelay() > 0 && progress.getNumberOfTimesCompleted() == 0) {
            LOGGER.debug("Homework " + homework.getId() + " has a delay of "
                    + homework.getDelay() + " minutes. Creating homework trigger...");
            final HomeworkTrigger trigger = new HomeworkTrigger();

            trigger.setProgress(entityUtil.getHomeworkProgressByHomeworkId(user, bot, homeworkId));
            trigger.setBot(bot);
            trigger.setUser(user);
            trigger.setCreatedAt(LocalDateTime.now());
            trigger.setTarget(LocalDateTime.now().plusMinutes(homework.getDelay()));

            homeworkTriggersRepository.save(trigger);

            LOGGER.debug("New trigger " + trigger.getId() + " for user " + user.getId()
                    + " and homework " + homework.getId() + " has been created. It will "
                    + "activate at " + trigger.getTarget() + ".");
            return Optional.of(trigger);
        }
        return Optional.empty();
    }

    @Transactional
    public BanTrigger createBanTrigger(UserEntity target, Bot bot, int hours, boolean isGeneral) {
        Assert.notNull(target, "target cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.state(hours > 0, "hours must be greater than 0");

        LOGGER.debug("Creating timed trigger for user " + target.getId() + "'s ban in bot " + bot + "...");
        final BanTrigger trigger = new BanTrigger();

        trigger.setBot(bot);
        trigger.setUser(target);
        trigger.setCreatedAt(LocalDateTime.now());
        trigger.setGeneral(isGeneral);
        trigger.setTarget(LocalDateTime.now().plusHours(hours));

        LOGGER.debug("New trigger's target will be " + trigger.getTarget() + ".");
        banTriggersRepository.save(trigger);
        LOGGER.debug("Timed trigger for user " + target.getId() + " has been created and persisted.");  

        return trigger;
    }

    @Transactional(readOnly = true)
    public Optional<LessonTrigger> findLessonTrigger(Long userId, Long courseId, int stage) {
        return lessonTriggersRepository.findByUserIdAndCourseIdAndProgressStage(userId, courseId, stage);
    }

    @Transactional(readOnly = true)
    public boolean existsHomeworkTrigger(Long userId, Long homeworkId) {
        return homeworkTriggersRepository.existsByUserIdAndProgressHomeworkId(userId, homeworkId);
    }

    @Transactional
    public int removeBanTriggerIfPresent(Long userId, Long botId) {
        return banTriggersRepository.deleteByUserIdAndBotId(userId, botId);
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
