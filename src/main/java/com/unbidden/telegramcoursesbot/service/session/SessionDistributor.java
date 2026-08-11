package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.exception.SessionException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.SessionRepository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class SessionDistributor {
    private static final Logger LOGGER = LogManager.getLogger(SessionDistributor.class);

    private final SessionRepository sessionRepository;

    private final UserOrChatRequestSessionService userOrChatRequestSessionService;

    private final ContentSessionService contentSessionService;

    private final LocalizationLoader localizationLoader;

    public void callService(UserEntity user, Bot bot, Message message) {
        LOGGER.debug("Looking for sessions...");
        final List<Session> userSessions = sessionRepository
                .findForUserInBot(message.getFrom().getId(), bot);

        if (userSessions.size() == 0) {
            return;
        }
        LOGGER.debug("Some sessions have been found.");
        final int numberOfUserOrChatRequestSessions = userSessions.stream()
                .filter(s -> s.getClass().equals(UserOrChatRequestSession.class))
                .toList()
                .size();

        if (numberOfUserOrChatRequestSessions == 0) {
            LOGGER.debug("There are no user or chat request sessions.");
            if (userSessions.size() != 1) {
                throw new SessionException("There is supposed to be only one content "
                        + "session", localizationLoader.localize(
                        Error.MORE_THEN_ONE_SESSION, user));
            }
            LOGGER.debug("Calling " + contentSessionService.getClass().getSimpleName()
                    + " to process message...");
            contentSessionService.processResponse(user, bot, userSessions.get(0), message);
        } else {
            if (numberOfUserOrChatRequestSessions == userSessions.size()) {
                final List<UserOrChatRequestSession> userOrChatRequestSessions =
                        userSessions.stream().map(s -> (UserOrChatRequestSession)s).toList();
                        
                if (message.getUsersShared() != null) {
                    LOGGER.debug("Sessions are of user request type.");
                    final List<UserOrChatRequestSession> userSharedSession = userOrChatRequestSessions.stream()
                            .filter(s -> s.getRequestId() == Integer.parseInt(message
                            .getUsersShared().getRequestId())).toList();
                            
                    if (userSharedSession.size() != 1) {
                        throw new SessionException("There is supposed to be only one users "
                                + "request session with id " + message.getUsersShared()
                                .getRequestId() + ". Collision might have occured",
                                localizationLoader.localize(
                                Error.MORE_THEN_ONE_SESSION, user));
                    }
                    LOGGER.debug("Calling " + userOrChatRequestSessionService.getClass()
                            .getSimpleName() + " to process message...");
                    userOrChatRequestSessionService.processResponse(user, bot, userSharedSession.get(0),
                            message);
                } else if (message.getChatShared() != null) {
                    LOGGER.debug("Sessions are of chat request type.");
                    final List<UserOrChatRequestSession> chatSharedSession = userOrChatRequestSessions.stream()
                            .filter(s -> s.getRequestId() == Integer.parseInt(message
                            .getChatShared().getRequestId())).toList();

                    if (chatSharedSession.size() != 1) {
                        throw new SessionException("There is supposed to be only one chat "
                                + "request session with id " + message.getChatShared()
                                .getRequestId() + ". Collision might have occured",
                                localizationLoader.localize(
                                Error.MORE_THEN_ONE_SESSION, user));
                    }
                    LOGGER.debug("Calling " + userOrChatRequestSessionService.getClass()
                            .getSimpleName() + " to process message...");
                    userOrChatRequestSessionService.processResponse(user, bot, chatSharedSession.get(0),
                            message);
                } else {
                    throw new SessionException("Sessions for user are of user or chat request "
                            + "type, but message does not contain any shared entity",
                            localizationLoader.localize(
                            Error.SESSION_NO_SHARED_ENTITY, user));
                }
            } else {
                throw new SessionException("User has user or chat request sessions mixed with "
                        + "content request sessions. This is not allowed", localizationLoader
                        .localize(Error.MIXED_SESSIONS, user));
            }
        }
    }

    public void removeSessionsForUser(UserEntity user, Bot bot) {
        contentSessionService.removeSessionsForUserInBot(user, bot);
    }

    public void removeSessionsWithoutConfirmationForUser(UserEntity user, Bot bot) {
        contentSessionService.removeSessionsWithoutConfirmationForUser(user, bot);
    }
}
