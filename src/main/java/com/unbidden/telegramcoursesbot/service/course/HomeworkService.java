package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface HomeworkService {
    void sendHomework(@NonNull UserEntity user, @NonNull Homework homework);

    void process(@NonNull Long id, @NonNull Message message);

    void commit(@NonNull Long id);

    void requestFeedback(@NonNull HomeworkProgress homeworkProgress);

    void approve(@NonNull Long id, @NonNull UserEntity user, @Nullable Message adminComment);

    void decline(@NonNull Long id, @NonNull UserEntity user, @NonNull Message adminComment);

    @NonNull
    Homework getHomework(@NonNull Long id);
}
