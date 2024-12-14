package com.unbidden.telegramcoursesbot.service.command;

import com.unbidden.telegramcoursesbot.exception.NoImplementationException;
import com.unbidden.telegramcoursesbot.model.Role;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.service.command.handler.CommandHandler;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommandHandlerManagerImpl implements CommandHandlerManager {
    private final List<CommandHandler> handlers;

    @Override
    @NonNull
    public CommandHandler getHandler(@NonNull String command) throws NoImplementationException {
        for (CommandHandler commandHandler : handlers) {
            if (commandHandler.getCommand().equals(command)) {
                return commandHandler;
            }
        }
        throw new NoImplementationException("There is no command handler for " + command);
    }

    @Override
    @NonNull
    public List<String> getCommandsForRole(@NonNull Role role) {
        final List<String> commands = new ArrayList<>();
        final List<AuthorityType> authorityTypes = role.getAuthorities().stream()
                .map(a -> a.getType()).toList();
        
        for (CommandHandler handler : handlers) {
            if (authorityTypes.containsAll(handler.getAuthorities())) {
                commands.add(handler.getCommand());
            }
        }
        return commands;
    }
}
