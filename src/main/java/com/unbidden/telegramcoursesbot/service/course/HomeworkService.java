package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface HomeworkService {
    void sendHomework(@NonNull UserEntity user, @NonNull Homework homework);

    void commit(@NonNull Long id, @NonNull List<Message> messages);

    void requestFeedback(@NonNull HomeworkProgress homeworkProgress);

    void approve(@NonNull Long id, @NonNull UserEntity user,
            @Nullable List<Message> adminComment);

    void decline(@NonNull Long id, @NonNull UserEntity user,
            @NonNull List<Message> adminComment);

    @NonNull
    Homework getHomework(@NonNull Long id);
}
