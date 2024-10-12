package com.unbidden.telegramcoursesbot.service.command;

import com.unbidden.telegramcoursesbot.service.command.handler.CommandHandler;
import java.util.List;
import org.springframework.lang.NonNull;

public interface CommandHandlerManager {
    @NonNull
    CommandHandler getHandler(@NonNull String command);

    @NonNull
    List<String> getAdminCommands();

    @NonNull
    List<String> getUserCommands();
}
