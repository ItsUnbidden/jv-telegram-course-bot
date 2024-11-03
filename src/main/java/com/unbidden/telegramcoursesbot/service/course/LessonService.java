package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import org.springframework.lang.NonNull;

public interface LessonService {
    @NonNull
    Lesson save(@NonNull Lesson lesson);

    @NonNull
    Lesson addContent(@NonNull Long lessonId, @NonNull LocalizedContent content);

    @NonNull
    Lesson removeContent(@NonNull Long lessonId, @NonNull Long mappingId);

    @NonNull
    Lesson moveContentToIndex(@NonNull Long lessonId, @NonNull Long mappingId, int index);

    @NonNull
    Lesson getById(@NonNull Long lessonId);
}
