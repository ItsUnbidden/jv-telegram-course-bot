package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.HomeworkTrigger;
import com.unbidden.telegramcoursesbot.model.LessonTrigger;
import com.unbidden.telegramcoursesbot.model.TimedTrigger;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.HomeworkTriggersRepository;
import com.unbidden.telegramcoursesbot.repository.LessonTriggersRepository;
import java.time.LocalDateTime;
import java.util.List;
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
    @Scheduled(initialDelay = INITIAL_TIMED_TRIGGER_CHECK_DELAY,
            fixedRateString = "${telegram.bot.message.course.trigger.schedule.delay}")
    public void checkTriggers() {
        LOGGER.debug("A scheduled check for expired timed triggers is commencing...");
        final List<LessonTrigger> expiredLessonTriggers =
                lessonTriggersRepository.findAllExpired();

        for (LessonTrigger trigger : expiredLessonTriggers) {
           processLessonTrigger(trigger);
        }
        LOGGER.debug(expiredLessonTriggers.size()
                + " expired lesson triggers have been processed.");
        final List<HomeworkTrigger> expiredHomeworkTriggers =
                homeworkTriggersRepository.findAllExpired();

        for (HomeworkTrigger trigger : expiredHomeworkTriggers) {
           processHomeworkTrigger(trigger);
        }
        LOGGER.debug(expiredHomeworkTriggers.size()
                + " expired homework triggers have been processed.");
    }

    @Override
    public boolean isWaitingForLesson(@NonNull UserEntity user,
            @NonNull CourseProgress progress) {
        return lessonTriggersRepository.findByCourseStageAndUser(user.getId(),
                progress.getCourse().getId(), progress.getStage()).isPresent();
    }

    @Override
    public boolean isWaitingForHomework(@NonNull UserEntity user, @NonNull Homework homework) {
        return homeworkTriggersRepository.findByHomeworkAndUser(user.getId(),
                homework.getId()).isPresent();
    }

    private void processLessonTrigger(LessonTrigger trigger) {
        LOGGER.trace("Lesson trigger " + trigger.getId() + " has expired.");
        courseService.current(trigger.getProgress());
        lessonTriggersRepository.delete(trigger);
    }

    private void processHomeworkTrigger(HomeworkTrigger trigger) {
        LOGGER.trace("Homework trigger " + trigger.getId() + " has expired.");
        homeworkService.sendHomework(trigger.getProgress().getUser(),
                trigger.getProgress().getHomework());
        homeworkTriggersRepository.delete(trigger);
    }
}
