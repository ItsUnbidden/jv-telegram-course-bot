package com.unbidden.telegramcoursesbot.service.command;

import com.unbidden.telegramcoursesbot.exception.NoImplementationException;
import com.unbidden.telegramcoursesbot.service.command.handler.CommandHandler;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommandHandlerManagerImpl implements CommandHandlerManager {
    @Autowired
    List<CommandHandler> handlers;

    @Override
    public CommandHandler getHandler(String command) {
        for (CommandHandler commandHandler : handlers) {
            if (commandHandler.getCommand().equals(command)) {
                return commandHandler;
            }
        }
        throw new NoImplementationException("There is no command handler for " + command);
    }
}
