package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.dto.internal.SessionParamsDto;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

import lombok.Data;

@Data
public abstract class Session {
    private UUID id;

    private UserEntity user;

    private Bot bot;

    private LocalDateTime timestamp;

    private Consumer<SessionParamsDto> function;
}
