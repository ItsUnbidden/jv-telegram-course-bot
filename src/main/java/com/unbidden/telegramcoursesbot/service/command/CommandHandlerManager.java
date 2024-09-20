package com.unbidden.telegramcoursesbot.service.command;

import com.unbidden.telegramcoursesbot.service.command.handler.CommandHandler;

public interface CommandHandlerManager {
    CommandHandler getHandler(String command);
}
