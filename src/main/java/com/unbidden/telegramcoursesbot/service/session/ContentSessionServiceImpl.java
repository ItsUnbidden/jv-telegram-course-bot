package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.SessionException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.localization.Localizations.Menu;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.SessionRepository;
import com.unbidden.telegramcoursesbot.service.menu.MenuKey;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.MenuTerminationGroupKey;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class ContentSessionServiceImpl implements ContentSessionService {
    private static final Logger LOGGER = LogManager.getLogger(ContentSessionServiceImpl.class);

    private final SessionRepository sessionRepository;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    public UUID createSession(UserEntity user, Bot bot, Consumer<List<Message>> function) {
        return createSession(user, bot, function, false);
    }

    @Override
    public UUID createSession(UserEntity user, Bot bot, Consumer<List<Message>> function, boolean isSkippingConfirmation) {
        sessionRepository.removeUserOrChatRequestSessionsForUserInBot(user.getId(), bot);
        final List<Session> sessions = sessionRepository.findForUserInBot(user.getId(), bot);

        if (sessions.size() > 1) {
            throw new SessionException("User " + user.getId() + " has more then one "
                    + "content session", null);
        } else if (sessions.size() == 1) {
            LOGGER.trace("User " + user.getId() + " already has a session "
                    + sessions.get(0).getId() + ".");
            return sessions.get(0).getId(); // TODO: potentially nonsense. If a user wants to start another session, the previous one will be returned instead, executing the wrong logic.
        }

        LOGGER.trace("Creating new content session for user " + user.getId() + "...");
        final ContentSession session = new ContentSession();

        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setBot(bot);
        session.setTimestamp(LocalDateTime.now());
        session.setFunction(function);
        session.setMessages(new ArrayList<>());
        session.setMenuInitialized(false);
        session.setSkippingConfirmation(isSkippingConfirmation);
        sessionRepository.save(session);
        LOGGER.trace("Session saved.");
        return session.getId();
    }

    @Override
    public void removeSessionsForUserInBot(UserEntity user, Bot bot) {
        sessionRepository.removeForUserInBot(user.getId(), bot);
    }

    @Override
    public void removeSessionsWithoutConfirmationForUser(UserEntity user, Bot bot) {
        sessionRepository.removeSessionsWithoutConfirmationForUserInBot(user.getId(), bot);
    }

    @Override
    public void processResponse(Session session, Message message) {
        final ContentSession contentSession = (ContentSession)session;
        
        contentSession.getMessages().add(message);
        LOGGER.trace("Adding new message to the confirmation list...");
        if (contentSession.isSkippingConfirmation()) {
            LOGGER.trace("Only one message is expected, no confirmation message will be sent.");
            commit(session.getId(), session.getUser());
        } else if (!contentSession.isMenuInitialized()) {
            LOGGER.trace("Sending confirmation menu...");
            final Message menuMessage = menuService.initiateMenu(contentSession.getUser(), session.getBot(),
                    MenuKey.COMMIT_CONTENT, contentSession.getId().toString());
            
            menuService.addToMenuTerminationGroup(session.getUser(), session.getUser(),
                    session.getBot(), menuMessage.getMessageId(), MenuTerminationGroupKey.COMMIT_CONTENT,
                    Menu.COMMIT_CONTENT_TERMINAL_PAGE, session.getId());
            contentSession.setMenuInitialized(true);
        }
        LOGGER.trace("Session response of user " + contentSession.getUser().getId()
                + " has been processed.");
    }

    @Override
    public void commit(UUID sessionId, UserEntity user) {
        final ContentSession session = (ContentSession)getSession(sessionId, user);

        LOGGER.trace("Removing sessions for user " + session.getUser().getId() + "...");
        if (!session.isSkippingConfirmation()) {
            menuService.terminateMenuGroup(user, session.getBot(),
            MenuTerminationGroupKey.COMMIT_CONTENT, session.getId());
        }
        removeSessionsForUserInBot(session.getUser(), session.getBot());
        LOGGER.trace("All sessions have been removed for user. Executing content session "
                + sessionId + "'s function for user " + session.getUser().getId() + "...");
        session.execute();
        LOGGER.trace("Content session " + sessionId + "'s function has been executed.");
    }

    @Override
    public void resend(UUID sessionId, UserEntity user) {
        final ContentSession session = (ContentSession)getSession(sessionId, user);

        LOGGER.trace("Removing sessions for user " + session.getUser().getId()
                + " and recreating session...");
        if (!session.isSkippingConfirmation()) {
            menuService.terminateMenuGroup(user, session.getBot(), MenuTerminationGroupKey.COMMIT_CONTENT,
                    localizationLoader.getLocalizationForUser(Menu.COMMIT_CONTENT_RESEND_TERMINAL_PAGE, user), session.getId());
        }
        removeSessionsForUserInBot(session.getUser(), session.getBot());
        createSession(session.getUser(), session.getBot(), session.getFunction());
        LOGGER.trace("All sessions have been removed for user and new session has been created. "
                + "Sending resend message...");

        final Localization resendLoc = localizationLoader.getLocalizationForUser(
                Localizations.Service.RESEND_CONTENT, session.getUser());
        clientManager.getClient(session.getBot()).sendMessage(SendMessage.builder()
                .chatId(session.getUser().getId())
                .text(resendLoc.getData())
                .entities(resendLoc.getEntities())
                .build());
        LOGGER.trace("Resend message has been sent.");
    }

    @Override
    public void cancel(UUID sessionId, UserEntity user) {
        final ContentSession session = (ContentSession)getSession(sessionId, user);

        if (!session.isSkippingConfirmation()) {
            menuService.terminateMenuGroup(user, session.getBot(), MenuTerminationGroupKey.COMMIT_CONTENT,
                    localizationLoader.getLocalizationForUser(Menu.COMMIT_CONTENT_CANCEL_TERMINAL_PAGE, user), session.getId());
        }
        removeSessionsForUserInBot(session.getUser(), session.getBot());
    }

    private Session getSession(UUID sessionId, UserEntity user) {
        final Optional<Session> potentialSession = sessionRepository.find(sessionId);

        if (potentialSession.isEmpty()) {
            throw new ActionExpiredException("There is no session with id " + sessionId
                    + ". It might have expired.", localizationLoader.getLocalizationForUser(
                    Error.SESSION_EXPIRED, user));
        }
        return potentialSession.get();
    }
}
