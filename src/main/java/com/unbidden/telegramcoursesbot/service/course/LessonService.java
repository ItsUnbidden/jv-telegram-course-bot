package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.exception.MoveContentException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import java.util.List;
import org.springframework.lang.NonNull;

public interface LessonService {
    @NonNull
    Lesson save(@NonNull Lesson lesson);

    @NonNull
    List<Lesson> saveAll(@NonNull List<Lesson> lessons);

    @NonNull
    Lesson addContent(@NonNull Long lessonId, @NonNull LocalizedContent content,
            @NonNull UserEntity user, @NonNull Bot bot);

    @NonNull
    Lesson removeContent(@NonNull Long lessonId, @NonNull Long mappingId,
            @NonNull UserEntity user, @NonNull Bot bot);

    @NonNull
    Lesson moveContentToIndex(@NonNull Long lessonId, @NonNull Long mappingId, int index,
            @NonNull UserEntity user, @NonNull Bot bot) throws MoveContentException;

    @NonNull
    Lesson getById(@NonNull Long lessonId, @NonNull UserEntity user, @NonNull Bot bot);

    @NonNull
    Lesson createLesson(@NonNull UserEntity user, @NonNull Course course, int position);

    @NonNull 
    Lesson removeLesson(@NonNull UserEntity user, @NonNull Lesson lesson);
}
