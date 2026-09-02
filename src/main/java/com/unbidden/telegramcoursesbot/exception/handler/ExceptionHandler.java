package com.unbidden.telegramcoursesbot.exception.handler;

import com.unbidden.telegramcoursesbot.model.BotRole;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface ExceptionHandler {
    SendMessage compileSendMessage(BotRole botRole, Exception exc);
}
