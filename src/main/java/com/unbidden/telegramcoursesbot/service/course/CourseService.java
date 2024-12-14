package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import org.springframework.lang.NonNull;

public interface CourseService {
    public static final String COURSE_NAME_LESSON_INDEX_DIVIDER = "/";

    public static final String COURSE_NEXT_STAGE_MENU_TERMINATION =
            "course_progress_%s_next_stage";

    void initMessage(@NonNull UserEntity user, @NonNull Bot bot, @NonNull String courseName);

    void next(@NonNull UserEntity user, @NonNull String courseName);

    void current(@NonNull CourseProgress courseProgress);

    void end(@NonNull UserEntity user, @NonNull CourseProgress courseProgress);

    @NonNull
    Course getCourseByName(@NonNull String courseName, @NonNull UserEntity user,
            @NonNull Bot bot);

    @NonNull
    Course getCourseById(@NonNull Long id, @NonNull UserEntity user, @NonNull Bot bot);

    @NonNull
    List<Course> getByBot(@NonNull Bot bot);

    @NonNull
    List<Course> getAllOwnedByUser(@NonNull UserEntity user, @NonNull Bot bot);

    @NonNull
    Course save(@NonNull Course course);

    boolean hasCourseBeenCompleted(@NonNull UserEntity user, @NonNull Course course);

    @NonNull
    CourseProgress getCurrentCourseProgressForUser(@NonNull Long userId,
            @NonNull String courseName);

    @NonNull
    CourseProgress getProgress(@NonNull Long id, @NonNull UserEntity user);

    void delete(@NonNull Course course, @NonNull UserEntity user);

    @NonNull
    Course createCourse(@NonNull Bot bot, @NonNull String courseName,
            @NonNull Integer price, @NonNull Integer amountOfLessons);

    @NonNull
    Course createInitialCourse(@NonNull Bot bot);

    boolean isDeletable(@NonNull Course course);

    void checkCourseIsNotUnderMaintenance(@NonNull Course course,
            @NonNull UserEntity user);
}
