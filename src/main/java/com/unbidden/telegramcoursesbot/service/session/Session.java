package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.dto.internal.SessionParamsDto;
import com.unbidden.telegramcoursesbot.model.BotRole;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

import lombok.Data;

@Data
public abstract class Session {
    private UUID id;

    private BotRole botRole;

    private LocalDateTime timestamp;

    private Consumer<SessionParamsDto> function;
}
