package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.Message;

@Data
public abstract class Session {
    private Integer id;

    private UserEntity user;

    private LocalDateTime timestamp;

    private Consumer<List<Message>> function;
}
