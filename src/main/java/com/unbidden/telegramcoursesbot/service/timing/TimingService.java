package com.unbidden.telegramcoursesbot.service.timing;

import com.unbidden.telegramcoursesbot.model.BanTrigger;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.HomeworkTrigger;
import com.unbidden.telegramcoursesbot.model.LessonTrigger;
import com.unbidden.telegramcoursesbot.model.TimedTrigger;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.Optional;
import org.springframework.lang.NonNull;

public interface TimingService {
    @NonNull
    TimedTrigger createTrigger(@NonNull CourseProgress progress);

    @NonNull
    TimedTrigger createTrigger(@NonNull HomeworkProgress progress);

    @NonNull
    TimedTrigger createTrigger(@NonNull UserEntity target, @NonNull Bot bot,
            int hours, boolean isGeneral);

    /**
     * This is called by scheduler and is not supposed to be called manually.
     */
    void checkTriggers();

    Optional<LessonTrigger> findLessonTrigger(@NonNull UserEntity user,
            @NonNull CourseProgress progress);

    Optional<HomeworkTrigger> findHomeworkTrigger(@NonNull UserEntity user,
            @NonNull Homework homework);

    Optional<BanTrigger> findBanTrigger(@NonNull UserEntity user, @NonNull Bot bot);

    int getTimeLeft(@NonNull TimedTrigger trigger);

    void removeTrigger(@NonNull BanTrigger banTrigger);
}
