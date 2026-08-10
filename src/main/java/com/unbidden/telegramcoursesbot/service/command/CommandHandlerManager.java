package com.unbidden.telegramcoursesbot.service.command;

import com.unbidden.telegramcoursesbot.exception.NoImplementationException;
import com.unbidden.telegramcoursesbot.model.Role;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.service.command.handler.CommandHandler;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class CommandHandlerManager {
    private final List<CommandHandler> handlers;

    public CommandHandler getHandler(String command) throws NoImplementationException {
        Assert.notNull(command, "command cannot be null");

        for (final CommandHandler commandHandler : handlers) {
            if (commandHandler.getCommand().equals(command)) {
                return commandHandler;
            }
        }
        throw new NoImplementationException("There is no command handler for " + command);
    }

    public List<String> getCommandsForRole(Role role) {
        Assert.notNull(role, "role cannot be null");

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
