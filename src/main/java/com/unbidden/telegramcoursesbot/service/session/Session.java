package com.unbidden.telegramcoursesbot.service.session;

import java.time.LocalDateTime;
import java.util.function.Consumer;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

@Data
public class Session {
    private User user;

    private LocalDateTime timestamp;

    private Consumer<Message> function;
}
