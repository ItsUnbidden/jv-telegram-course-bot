package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.exception.MoveContentException;
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
            @NonNull UserEntity user);

    @NonNull
    Lesson removeContent(@NonNull Long lessonId, @NonNull Long mappingId,
            @NonNull UserEntity user);

    @NonNull
    Lesson moveContentToIndex(@NonNull Long lessonId, @NonNull Long mappingId, int index,
            @NonNull UserEntity user) throws MoveContentException;

    @NonNull
    Lesson getById(@NonNull Long lessonId, @NonNull UserEntity user);
}
