package com.unbidden.telegramcoursesbot.service.statistics;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import org.springframework.lang.NonNull;

public interface StatisticsService {
    void sendBotStatistics(@NonNull UserEntity user, @NonNull Bot bot);

    void sendBotUsers(@NonNull UserEntity user, @NonNull Bot bot);

    void sendCourseStatistics(@NonNull UserEntity user, @NonNull Course course);

    void sendCourseUsers(@NonNull UserEntity user, @NonNull Course course);

    void sendCourseCompletedUsers(@NonNull UserEntity user, @NonNull Course course);

    void sendCourseStageUsers(@NonNull UserEntity user, @NonNull Course course, int stage);
}
