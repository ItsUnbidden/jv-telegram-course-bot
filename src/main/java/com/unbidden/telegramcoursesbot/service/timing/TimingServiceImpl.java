package com.unbidden.telegramcoursesbot.service.timing;

import com.unbidden.telegramcoursesbot.model.BanTrigger;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.HomeworkTrigger;
import com.unbidden.telegramcoursesbot.model.LessonTrigger;
import com.unbidden.telegramcoursesbot.model.TimedTrigger;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.BanTriggersRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkTriggersRepository;
import com.unbidden.telegramcoursesbot.repository.LessonTriggersRepository;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.course.HomeworkService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimingServiceImpl implements TimingService {
    private static final Logger LOGGER = LogManager.getLogger(TimingServiceImpl.class);

    private static final int INITIAL_TIMED_TRIGGER_CHECK_DELAY = 10000;

    private final LessonTriggersRepository lessonTriggersRepository;

    private final HomeworkTriggersRepository homeworkTriggersRepository;

    private final BanTriggersRepository banTriggersRepository;

    @Autowired
    @Lazy
    private UserService userService;

    @Autowired
    @Lazy
    private CourseService courseService;

    @Autowired
    @Lazy
    private HomeworkService homeworkService;

    @Override
    @NonNull
    public TimedTrigger createTrigger(@NonNull CourseProgress progress) {
        LOGGER.debug("Creating timed trigger for lesson " + progress.getStage()
                + " in course " + progress.getCourse().getName() + " for user "
                + progress.getUser().getId() + "...");
        final LessonTrigger trigger = new LessonTrigger();

        trigger.setProgress(progress);
        trigger.setBot(progress.getCourse().getBot());
        trigger.setUser(progress.getUser());
        trigger.setCreatedAt(LocalDateTime.now());

        trigger.setTarget(LocalDateTime.now().plusMinutes(progress.getCourse().getLessons()
                .get(progress.getStage()).getDelay()));
        LOGGER.debug("New trigger's target will be " + trigger.getTarget() + ".");
        lessonTriggersRepository.save(trigger);
        LOGGER.debug("Timed trigger for user " + progress.getUser().getId()
                + " has been created and persisted.");  
        return trigger;
    }

    @Override
    @NonNull
    public TimedTrigger createTrigger(@NonNull HomeworkProgress progress) {
        LOGGER.debug("Creating timed trigger for homework in lesson with id " + progress
                .getHomework().getLesson().getId() + " for user "
                + progress.getUser().getId() + "...");
        final HomeworkTrigger trigger = new HomeworkTrigger();

        trigger.setProgress(progress);
        trigger.setBot(progress.getHomework().getLesson().getCourse().getBot());
        trigger.setUser(progress.getUser());
        trigger.setCreatedAt(LocalDateTime.now());

        trigger.setTarget(LocalDateTime.now().plusMinutes(progress.getHomework().getDelay()));
        LOGGER.debug("New trigger's target will be " + trigger.getTarget() + ".");
        homeworkTriggersRepository.save(trigger);
        LOGGER.debug("Timed trigger for user " + progress.getUser().getId()
                + " has been created and persisted.");  
        return trigger;
    }

    @Override
    @NonNull
    public TimedTrigger createTrigger(@NonNull UserEntity target, @NonNull Bot bot,
            int hours, boolean isGeneral) {
        LOGGER.debug("Creating timed trigger for user " + target.getId()
                + "'s ban in bot " + bot + "...");
        final BanTrigger trigger = new BanTrigger();

        trigger.setBot(bot);
        trigger.setUser(target);
        trigger.setCreatedAt(LocalDateTime.now());
        trigger.setGeneral(isGeneral);

        trigger.setTarget(LocalDateTime.now().plusHours(hours));
        LOGGER.debug("New trigger's target will be " + trigger.getTarget() + ".");
        banTriggersRepository.save(trigger);
        LOGGER.debug("Timed trigger for user " + target.getId()
                + " has been created and persisted.");  
        return trigger;
    }

    @Override
    @Scheduled(initialDelay = INITIAL_TIMED_TRIGGER_CHECK_DELAY,
            fixedRateString = "${telegram.bot.message.course.trigger.schedule.delay}")
    public void checkTriggers() {
        LOGGER.trace("A scheduled check for expired timed triggers is commencing...");
        final List<LessonTrigger> expiredLessonTriggers =
                lessonTriggersRepository.findAllExpired(LocalDateTime.now());

        for (LessonTrigger trigger : expiredLessonTriggers) {
           processLessonTrigger(trigger);
        }
        LOGGER.trace(expiredLessonTriggers.size()
                + " expired lesson triggers have been processed.");
        final List<HomeworkTrigger> expiredHomeworkTriggers =
                homeworkTriggersRepository.findAllExpired(LocalDateTime.now());

        for (HomeworkTrigger trigger : expiredHomeworkTriggers) {
           processHomeworkTrigger(trigger);
        }
        LOGGER.trace(expiredHomeworkTriggers.size()
                + " expired homework triggers have been processed.");

        final List<BanTrigger> expiredBanTriggers = banTriggersRepository
                .findAllExpired(LocalDateTime.now());

        for (BanTrigger trigger : expiredBanTriggers) {
           processBanTrigger(trigger);
        }
        LOGGER.trace(expiredBanTriggers.size()
                + " expired ban triggers have been processed.");
    }

    @Override
    public Optional<LessonTrigger> findLessonTrigger(@NonNull UserEntity user,
            @NonNull CourseProgress progress) {
        return lessonTriggersRepository.findByCourseStageAndUser(user.getId(),
                progress.getCourse().getId(), progress.getStage());
    }

    @Override
    public Optional<HomeworkTrigger> findHomeworkTrigger(@NonNull UserEntity user,
            @NonNull Homework homework) {
        return homeworkTriggersRepository.findByHomeworkAndUser(user.getId(), homework.getId());
    }

    @Override
    public Optional<BanTrigger> findBanTrigger(@NonNull UserEntity user, @NonNull Bot bot) {
        return banTriggersRepository.findByBotAndUser(bot, user);
    }

    /**
     * Returns the amount of time left until this {@link TimedTrigger} will activate.
     * Calculated in hours, where 0 means less then an hour.
     */
    @Override
    public int getTimeLeft(@NonNull TimedTrigger trigger) {
        return (int)trigger.getTarget().until(LocalDateTime.now(), ChronoUnit.HOURS);
    }

    @Override
    public void removeTrigger(@NonNull BanTrigger banTrigger) {
        banTriggersRepository.delete(banTrigger);
    }

    private void processLessonTrigger(LessonTrigger trigger) {
        LOGGER.trace("Lesson trigger " + trigger.getId() + " has expired.");
        if (trigger.getProgress().getCourse().isUnderMaintenance()) {
            LOGGER.warn("Lesson trigger " + trigger.getId() + " cannot be activated "
                    + "because course " + trigger.getProgress().getCourse().getName()
                    + " is under maintenance.");
            return;
        }
        lessonTriggersRepository.delete(trigger);
        courseService.current(courseService.getProgress(trigger.getProgress().getId(),
                trigger.getUser()));
    }

    private void processHomeworkTrigger(HomeworkTrigger trigger) {
        LOGGER.trace("Homework trigger " + trigger.getId() + " has expired.");
        final HomeworkProgress progress = homeworkService.getProgress(trigger.getProgress()
                .getId(), trigger.getUser());
        final Course course = progress.getHomework().getLesson().getCourse();
        if (course.isUnderMaintenance()) {
            LOGGER.warn("Homework trigger " + trigger.getId() + " cannot be activated "
                    + "because course " + course.getName() + " is under maintenance.");
            return;
        }
        homeworkTriggersRepository.delete(trigger);
        homeworkService.sendHomework(progress);
    }

    private void processBanTrigger(BanTrigger trigger) {
        LOGGER.trace("Ban trigger " + trigger.getId() + " has expired.");
        banTriggersRepository.delete(trigger);
        if (trigger.isGeneral()) {
            userService.liftGeneralBan(null, trigger.getUser());
        } else {
            userService.liftBanInBot(null, trigger.getUser(), trigger.getBot());
        }
    }
}
