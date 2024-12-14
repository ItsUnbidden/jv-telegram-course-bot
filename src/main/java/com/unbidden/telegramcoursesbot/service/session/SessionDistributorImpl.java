package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.exception.SessionException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.SessionRepository;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class SessionDistributorImpl implements SessionDistributor {
    private static final Logger LOGGER = LogManager.getLogger(SessionDistributorImpl.class);

    private static final String ERROR_MIXED_SESSIONS = "error_mixed_sessions";
    private static final String ERROR_SESSION_NO_SHARED_ENTITY = "error_session_no_shared_entity";
    private static final String ERROR_MORE_THEN_ONE_SESSION = "error_more_then_one_session";

    private final SessionRepository sessionRepository;

    private final UserOrChatRequestSessionService userOrChatRequestSessionService;

    private final ContentSessionService contentSessionService;

    private final LocalizationLoader localizationLoader;

    @Override
    public void callService(@NonNull Message message, @NonNull UserEntity user,
            @NonNull Bot bot) {
        LOGGER.debug("Looking for sessions...");
        final List<Session> userSessions = sessionRepository
                .findForUserInBot(message.getFrom().getId(), bot);
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
                throw new SessionException("There is supposed to be only one content "
                        + "session", localizationLoader.getLocalizationForUser(
                        ERROR_MORE_THEN_ONE_SESSION, user));
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
                                .getRequestId() + ". Collision might have occured",
                                localizationLoader.getLocalizationForUser(
                                ERROR_MORE_THEN_ONE_SESSION, user));
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
                                .getRequestId() + ". Collision might have occured",
                                localizationLoader.getLocalizationForUser(
                                ERROR_MORE_THEN_ONE_SESSION, user));
                    }
                    LOGGER.debug("Calling " + userOrChatRequestSessionService.getClass()
                            .getSimpleName() + " to process message...");
                    userOrChatRequestSessionService.processResponse(chatSharedSession.get(0),
                            message);
                } else {
                    throw new SessionException("Sessions for user are of user or chat request "
                            + "type, but message does not contain any shared entity",
                            localizationLoader.getLocalizationForUser(
                            ERROR_SESSION_NO_SHARED_ENTITY, user));
                }
            } else {
                throw new SessionException("User has user or chat request sessions mixed with "
                        + "content request sessions. This is not allowed", localizationLoader
                        .getLocalizationForUser(ERROR_MIXED_SESSIONS, user));
            }
        }
    }

    @Override
    public void removeSessionsForUser(@NonNull UserEntity user, @NonNull Bot bot) {
        contentSessionService.removeSessionsForUserInBot(user, bot);
    }

    @Override
    public void removeSessionsWithoutConfirmationForUser(@NonNull UserEntity user,
            @NonNull Bot bot) {
        contentSessionService.removeSessionsWithoutConfirmationForUser(user, bot);
    }
}
