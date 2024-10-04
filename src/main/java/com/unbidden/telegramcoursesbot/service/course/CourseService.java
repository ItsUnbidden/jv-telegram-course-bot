package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import java.util.List;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.User;

public interface CourseService {
    void initMessage(@NonNull User user, @NonNull String courseName);

    void next(@NonNull UserEntity user, @NonNull String courseName);

    void current(@NonNull Course course, @NonNull CourseProgress courseProgress);

    void end(@NonNull UserEntity user, @NonNull CourseProgress courseProgress);

    @NonNull
    Course getCourseByName(@NonNull String courseName);

    @NonNull
    List<Course> getCourses();

    @NonNull
    Course save(@NonNull Course course);
}
