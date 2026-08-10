package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;

import java.util.List;

import org.telegram.telegrambots.meta.api.objects.message.Message;


/**
 * An interface that defines the behavior of a Telegram Bot command handler. 
 */
public interface CommandHandler {
    /**
     * This method is executed when a message with this handler's command arrives.
     * @param user who sent the request
     * @param bot in which the command was sent
     * @param message the command message
     * @param commandParts — contains the components of the sent command. The first element of the array is always the command itself.
     */
    void handle(UserEntity user, Bot bot, Message message, String[] commandParts);

    /**
     * Returns this handler's command.
     * @return the full command name
     */
    String getCommand();

    /**
     * Returns a list of authority types that the user must have in order to be allowed to execute this command.
     * @return a list of authority types
     */
    List<AuthorityType> getAuthorities();
}
