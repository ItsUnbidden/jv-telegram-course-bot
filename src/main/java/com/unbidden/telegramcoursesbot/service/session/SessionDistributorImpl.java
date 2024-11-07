package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.exception.SessionException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.SessionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class SessionDistributorImpl implements SessionDistributor {
    private static final Logger LOGGER = LogManager.getLogger(SessionDistributorImpl.class);

    private final SessionRepository sessionRepository;

    private final UserOrChatRequestSessionService userOrChatRequestSessionService;

    private final ContentSessionService contentSessionService;

    @Override
    public void callService(@NonNull Message message) {
        LOGGER.debug("Looking for sessions...");
        final List<Session> userSessions = sessionRepository
                .findForUser(message.getFrom().getId());
        if (userSessions.size() == 0) {
            return;
        }
        LOGGER.debug("Some sessions have been found.");
        final int amountOfUserOrChatRequestSessions = userSessions.stream()
                .filter(s -> s.getClass().equals(UserOrChatRequestSession.class))
                .toList()
                .size();
        if (amountOfUserOrChatRequestSessions == 0) {
            LOGGER.debug("There are no user or chat request sessions.");
            if (userSessions.size() != 1) {
                throw new SessionException("There is supposed to be only one content session");
            }
            LOGGER.debug("Calling " + contentSessionService.getClass().getSimpleName()
                    + " to process message...");
            contentSessionService.processResponse(userSessions.get(0), message);
        } else {
            if (amountOfUserOrChatRequestSessions == userSessions.size()) {
                if (message.getUsersShared() != null) {
                    LOGGER.debug("Sessions are of user request type.");
                    final List<Session> userSharedSession = userSessions.stream()
                            .filter(s -> s.getId().equals(Integer.parseInt(message
                            .getUsersShared().getRequestId())))
                            .toList();
                    if (userSharedSession.size() != 1) {
                        throw new SessionException("There is supposed to be only one users "
                                + "request session with id " + message.getUsersShared()
                                .getRequestId() + ". Collision might have occured");
                    }
                    LOGGER.debug("Calling " + userOrChatRequestSessionService.getClass()
                            .getSimpleName() + " to process message...");
                    userOrChatRequestSessionService.processResponse(userSharedSession.get(0),
                            message);
                } else if (message.getChatShared() != null) {
                    LOGGER.debug("Sessions are of chat request type.");
                    final List<Session> chatSharedSession = userSessions.stream()
                            .filter(s -> s.getId().equals(Integer.parseInt(message
                            .getChatShared().getRequestId())))
                            .toList();
                    if (chatSharedSession.size() != 1) {
                        throw new SessionException("There is supposed to be only one chat "
                                + "request session with id " + message.getChatShared()
                                .getRequestId() + ". Collision might have occured");
                    }
                    LOGGER.debug("Calling " + userOrChatRequestSessionService.getClass()
                            .getSimpleName() + " to process message...");
                    userOrChatRequestSessionService.processResponse(chatSharedSession.get(0),
                            message);
                } else {
                    throw new SessionException("Sessions for user are of user or chat request "
                            + "type, but message does not contain any shared entity");
                }
            } else {
                throw new SessionException("User has user or chat request sessions mixed with "
                        + "content request sessions. This is not allowed");
            }
        }
    }

    @Override
    public void removeSessionsForUser(@NonNull UserEntity user) {
        contentSessionService.removeSessionsForUser(user);
    }

    @Override
    public void removeSessionsWithoutConfirmationForUser(@NonNull UserEntity user) {
        contentSessionService.removeSessionsWithoutConfirmationForUser(user);
    }
}
