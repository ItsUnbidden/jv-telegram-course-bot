package com.unbidden.telegramcoursesbot.service.command;

import com.unbidden.telegramcoursesbot.exception.NoImplementationException;
import com.unbidden.telegramcoursesbot.service.command.handler.CommandHandler;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class CommandHandlerManagerImpl implements CommandHandlerManager {
    @Autowired
    private List<CommandHandler> handlers;

    @Override
    @NonNull
    public CommandHandler getHandler(@NonNull String command) {
        for (CommandHandler commandHandler : handlers) {
            if (commandHandler.getCommand().equals(command)) {
                return commandHandler;
            }
        }
        throw new NoImplementationException("There is no command handler for " + command);
    }

    @Override
    @NonNull
    public List<String> getAdminCommands() {
        return handlers.stream()
                .filter(h -> h.isAdminCommand())
                .map(h -> h.getCommand())
                .toList();
    }

    @Override
    @NonNull
    public List<String> getUserCommands() {
        return handlers.stream()
                .filter(h -> !h.isAdminCommand())
                .map(h -> h.getCommand())
                .toList();
    }

    @Override
    @NonNull
    public List<String> getAllCommands() {
        final List<String> commandNames = new ArrayList<>(getAdminCommands());
        commandNames.addAll(getUserCommands());

        return commandNames;
    }
}
