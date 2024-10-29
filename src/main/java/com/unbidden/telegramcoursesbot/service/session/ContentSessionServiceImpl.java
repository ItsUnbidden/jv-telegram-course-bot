package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.SessionException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.SessionRepository;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
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
import org.telegram.telegrambots.meta.api.objects.Message;

@Service
@RequiredArgsConstructor
public class ContentSessionServiceImpl implements ContentSessionService {
    private static final Logger LOGGER = LogManager.getLogger(ContentSessionServiceImpl.class);

    private static final String CONFIRMATION_MENU = "m_cmtCnt";

    private static final String SERVICE_RESEND_CONTENT = "service_resend_content";

    private final SessionRepository sessionRepository;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    @NonNull
    public Integer createSession(@NonNull UserEntity user,
            @NonNull Consumer<List<Message>> function) {
        return createSession(user, function, false);
    }

    @Override
    public Integer createSession(@NonNull UserEntity user, @NonNull Consumer<List<Message>> function,
            boolean isSkippingConfirmation) {
        sessionRepository.removeUserOrChatRequestSessionsForUser(user.getId());
        final List<Session> sessions = sessionRepository.findForUser(user.getId());
        if (sessions.size() > 1) {
            throw new SessionException("User " + user.getId() + " has more then one "
                    + "content session");
        } else if (sessions.size() == 1) {
            LOGGER.debug("User " + user.getId() + " already has a session "
                    + sessions.get(0).getId() + ".");
            return sessions.get(0).getId();
        }

        LOGGER.debug("Creating new content session for user " + user.getId() + "...");
        final ContentSession session = new ContentSession();
        session.setId(ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
        session.setUser(user);
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
    public void removeSessionsForUser(@NonNull UserEntity user) {
        sessionRepository.removeForUser(user.getId());
    }

    @Override
    public void processResponse(@NonNull Session session, @NonNull Message message) {
        final ContentSession contentSession = (ContentSession)session;
        
        contentSession.getMessages().add(message);
        LOGGER.debug("Adding new message to the confirmation list...");
        if (contentSession.isSkippingConfirmation()) {
            LOGGER.debug("Only one message is expected, no confirmation message will be sent.");
            commit(session.getId());
        } else if (!contentSession.isMenuInitialized()) {
            LOGGER.debug("Sending confirmation menu...");
            menuService.initiateMenu(CONFIRMATION_MENU, contentSession.getUser(),
                    contentSession.getId().toString());
            contentSession.setMenuInitialized(true);
        }
        LOGGER.debug("Session response of user " + contentSession.getUser().getId()
                + " has been processed.");
    }

    @Override
    public void commit(@NonNull Integer sessionId) {
        final ContentSession session = (ContentSession)getSession(sessionId);

        LOGGER.debug("Executing content session " + sessionId + "'s function' for user "
                + session.getUser().getId() + "...");
        session.execute();
        LOGGER.debug("Session " + sessionId + "'s function has been executed. "
                + "Removing sessions for user " + session.getUser().getId() + "...");
        removeSessionsForUser(session.getUser());
        LOGGER.debug("All sessions have been removed for user.");
    }

    @Override
    public void resend(@NonNull Integer sessionId) {
        final ContentSession session = (ContentSession)getSession(sessionId);

        LOGGER.debug("Removing sessions for user " + session.getUser().getId()
                + " and recreating session...");
        removeSessionsForUser(session.getUser());
        createSession(session.getUser(), session.getFunction());
        LOGGER.debug("All sessions have been removed for user and new session has been created. "
                + "Sending resend message...");

        final Localization resendLoc = localizationLoader.getLocalizationForUser(
                SERVICE_RESEND_CONTENT, session.getUser());
        bot.sendMessage(SendMessage.builder()
                .chatId(session.getUser().getId())
                .text(resendLoc.getData())
                .entities(resendLoc.getEntities())
                .build());
        LOGGER.debug("Resend message has been sent.");
    }

    @Override
    public void cancel(@NonNull Integer sessionId) {
        final ContentSession session = (ContentSession)getSession(sessionId);

        removeSessionsForUser(session.getUser());
    }

    private Session getSession(Integer sessionId) {
        final Optional<Session> potentialSession = sessionRepository.find(sessionId);

        if (potentialSession.isEmpty()) {
            throw new ActionExpiredException("There is no session with id " + sessionId
                    + ". It might have expired.");
        }
        return potentialSession.get();
    }
}
