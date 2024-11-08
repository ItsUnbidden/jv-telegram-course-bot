package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.model.UserEntity;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface ExceptionHandler {
    SendMessage compileSendMessage(@NonNull UserEntity user, @NonNull Exception exc);
}
