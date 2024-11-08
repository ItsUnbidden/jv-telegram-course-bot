package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.User;

public interface CourseService {
    public static final String COURSE_NAME_LESSON_INDEX_DIVIDER = "/";

    public static final String COURSE_NEXT_STAGE_MENU_TERMINATION =
            "course_progress_%s_next_stage";

    void initMessage(@NonNull User user, @NonNull String courseName);

    void initMessage(@NonNull UserEntity user, @NonNull String courseName);

    void next(@NonNull UserEntity user, @NonNull String courseName);

    void current(@NonNull Course course, @NonNull CourseProgress courseProgress);

    void end(@NonNull UserEntity user, @NonNull CourseProgress courseProgress);

    @NonNull
    Course getCourseByName(@NonNull String courseName, @NonNull UserEntity user);

    @NonNull
    Course getCourseById(@NonNull Long id, @NonNull UserEntity user);

    @NonNull
    List<Course> getAll();

    @NonNull
    List<Course> getAllOwnedByUser(@NonNull UserEntity user);

    @NonNull
    Course save(@NonNull Course course);

    boolean hasCourseBeenCompleted(@NonNull UserEntity user, @NonNull Course course);

    @NonNull
    CourseProgress getCurrentCourseProgressForUser(@NonNull Long userId,
            @NonNull String courseName);

    void delete(@NonNull Course course);
}
