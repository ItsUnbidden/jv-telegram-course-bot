package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

import org.apache.commons.lang3.function.TriConsumer;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Data
public abstract class Session {
    private UUID id;

    private UserEntity user;

    private Bot bot;

    private LocalDateTime timestamp;

    private TriConsumer<UserEntity, Bot, List<Message>> function;
}
