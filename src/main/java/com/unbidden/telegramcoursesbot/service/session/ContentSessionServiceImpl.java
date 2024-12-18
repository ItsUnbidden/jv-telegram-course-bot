package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.SessionException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.SessionRepository;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class ContentSessionServiceImpl implements ContentSessionService {
    private static final Logger LOGGER = LogManager.getLogger(ContentSessionServiceImpl.class);

    private static final String CONFIRMATION_MENU = "m_cmtCnt";

    private static final String SERVICE_RESEND_CONTENT = "service_resend_content";

    private static final String ERROR_SESSION_EXPIRED = "error_session_expired";

    private static final String MENU_COMMIT_CONTENT_TERMINAL_PAGE =
            "menu_commit_content_terminal_page";
    private static final String MENU_COMMIT_CONTENT_RESEND_TERMINAL_PAGE =
            "menu_commit_content_resend_terminal_page";
    private static final String MENU_COMMIT_CONTENT_CANCEL_TERMINAL_PAGE =
            "menu_commit_content_cancel_terminal_page";

    private static final String CONFIRM_MENU_TERMINATOR = "session_%s_terminator";

    private final SessionRepository sessionRepository;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @NonNull
    public Integer createSession(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull Consumer<List<Message>> function) {
        return createSession(user, bot, function, false);
    }

    @Override
    public Integer createSession(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull Consumer<List<Message>> function, boolean isSkippingConfirmation) {
        sessionRepository.removeUserOrChatRequestSessionsForUserInBot(user.getId(), bot);
        final List<Session> sessions = sessionRepository.findForUserInBot(user.getId(), bot);
        if (sessions.size() > 1) {
            throw new SessionException("User " + user.getId() + " has more then one "
                    + "content session", null);
        } else if (sessions.size() == 1) {
            LOGGER.debug("User " + user.getId() + " already has a session "
                    + sessions.get(0).getId() + ".");
            return sessions.get(0).getId();
        }

        LOGGER.debug("Creating new content session for user " + user.getId() + "...");
        final ContentSession session = new ContentSession();
        session.setId(ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
        session.setUser(user);
        session.setBot(bot);
        session.setTimestamp(LocalDateTime.now());
        session.setFunction(function);
        session.setMessages(new ArrayList<>());
        session.setMenuInitialized(false);
        session.setSkippingConfirmation(isSkippingConfirmation);
        sessionRepository.save(session);
        LOGGER.debug("Session saved.");
        return session.getId();
    }

    @Override
    public void removeSessionsForUserInBot(@NonNull UserEntity user, @NonNull Bot bot) {
        sessionRepository.removeForUserInBot(user.getId(), bot);
    }

    @Override
    public void removeSessionsWithoutConfirmationForUser(@NonNull UserEntity user,
            @NonNull Bot bot) {
        sessionRepository.removeSessionsWithoutConfirmationForUserInBot(user.getId(), bot);
    }

    @Override
    public void processResponse(@NonNull Session session, @NonNull Message message) {
        final ContentSession contentSession = (ContentSession)session;
        
        contentSession.getMessages().add(message);
        LOGGER.debug("Adding new message to the confirmation list...");
        if (contentSession.isSkippingConfirmation()) {
            LOGGER.debug("Only one message is expected, no confirmation message will be sent.");
            commit(session.getId(), session.getUser());
        } else if (!contentSession.isMenuInitialized()) {
            LOGGER.debug("Sending confirmation menu...");
            final Message menuMessage = menuService.initiateMenu(CONFIRMATION_MENU,
                    contentSession.getUser(), contentSession.getId().toString(),
                    session.getBot());
            menuService.addToMenuTerminationGroup(session.getUser(), session.getUser(),
                    session.getBot(), menuMessage.getMessageId(), CONFIRM_MENU_TERMINATOR
                    .formatted(session.getId()), MENU_COMMIT_CONTENT_TERMINAL_PAGE);
            contentSession.setMenuInitialized(true);
        }
        LOGGER.debug("Session response of user " + contentSession.getUser().getId()
                + " has been processed.");
    }

    @Override
    public void commit(@NonNull Integer sessionId, @NonNull UserEntity user) {
        final ContentSession session = (ContentSession)getSession(sessionId, user);

        LOGGER.debug("Removing sessions for user " + session.getUser().getId() + "...");
        if (!session.isSkippingConfirmation()) {
            menuService.terminateMenuGroup(user, session.getBot(), CONFIRM_MENU_TERMINATOR
                    .formatted(session.getId()));
        }
        removeSessionsForUserInBot(session.getUser(), session.getBot());
        LOGGER.debug("All sessions have been removed for user. Executing content session "
                + sessionId + "'s function for user " + session.getUser().getId() + "...");
        session.execute();
        LOGGER.debug("Content session " + sessionId + "'s function has been executed.");
    }

    @Override
    public void resend(@NonNull Integer sessionId, @NonNull UserEntity user) {
        final ContentSession session = (ContentSession)getSession(sessionId, user);

        LOGGER.debug("Removing sessions for user " + session.getUser().getId()
                + " and recreating session...");
        if (!session.isSkippingConfirmation()) {
            menuService.terminateMenuGroup(user, session.getBot(), CONFIRM_MENU_TERMINATOR
                    .formatted(session.getId()), localizationLoader.getLocalizationForUser(
                    MENU_COMMIT_CONTENT_RESEND_TERMINAL_PAGE, user));
        }
        removeSessionsForUserInBot(session.getUser(), session.getBot());
        createSession(session.getUser(), session.getBot(), session.getFunction());
        LOGGER.debug("All sessions have been removed for user and new session has been created. "
                + "Sending resend message...");

        final Localization resendLoc = localizationLoader.getLocalizationForUser(
                SERVICE_RESEND_CONTENT, session.getUser());
        clientManager.getClient(session.getBot()).sendMessage(SendMessage.builder()
                .chatId(session.getUser().getId())
                .text(resendLoc.getData())
                .entities(resendLoc.getEntities())
                .build());
        LOGGER.debug("Resend message has been sent.");
    }

    @Override
    public void cancel(@NonNull Integer sessionId, @NonNull UserEntity user) {
        final ContentSession session = (ContentSession)getSession(sessionId, user);

        if (!session.isSkippingConfirmation()) {
            menuService.terminateMenuGroup(user, session.getBot(), CONFIRM_MENU_TERMINATOR
                    .formatted(session.getId()), localizationLoader.getLocalizationForUser(
                    MENU_COMMIT_CONTENT_CANCEL_TERMINAL_PAGE, user));
        }
        removeSessionsForUserInBot(session.getUser(), session.getBot());
    }

    private Session getSession(Integer sessionId, UserEntity user) {
        final Optional<Session> potentialSession = sessionRepository.find(sessionId);

        if (potentialSession.isEmpty()) {
            throw new ActionExpiredException("There is no session with id " + sessionId
                    + ". It might have expired.", localizationLoader.getLocalizationForUser(
                    ERROR_SESSION_EXPIRED, user));
        }
        return potentialSession.get();
    }
}
