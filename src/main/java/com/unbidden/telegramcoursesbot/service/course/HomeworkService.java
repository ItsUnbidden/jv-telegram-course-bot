package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface HomeworkService {
    void sendHomework(@NonNull UserEntity user, @NonNull Homework homework);

    void commit(@NonNull Long id, @NonNull List<Message> messages);

    boolean requestFeedback(@NonNull HomeworkProgress homeworkProgress);

    void approve(@NonNull Long id, @NonNull UserEntity user,
            @Nullable List<Message> adminComment);

    void decline(@NonNull Long id, @NonNull UserEntity user,
            @NonNull List<Message> adminComment);

    @NonNull
    ContentMapping updateContent(@NonNull Long homeworkId, @NonNull LocalizedContent content,
            @NonNull UserEntity user);

    @NonNull
    Homework getHomework(@NonNull Long id, @NonNull UserEntity user);

    @NonNull
    Homework save(@NonNull Homework homework);

    @NonNull
    Homework createDefault(@NonNull Lesson lesson, @NonNull LocalizedContent content);
}
