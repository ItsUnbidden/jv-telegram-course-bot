package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.TimedTrigger;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import org.springframework.lang.NonNull;

public interface TimingService {
    @NonNull
    TimedTrigger createTrigger(@NonNull CourseProgress progress);

    @NonNull
    TimedTrigger createTrigger(@NonNull HomeworkProgress progress);

    /**
     * This is called by scheduler and is not supposed to be called manually.
     */
    void checkTriggers();

    boolean isWaitingForLesson(@NonNull UserEntity user, @NonNull CourseProgress progress);

    boolean isWaitingForHomework(@NonNull UserEntity user, @NonNull Homework homework);
}
