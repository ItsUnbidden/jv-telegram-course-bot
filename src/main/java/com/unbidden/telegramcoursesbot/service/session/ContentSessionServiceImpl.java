package com.unbidden.telegramcoursesbot.service.session;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.SessionParamsDto;
import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.SessionException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.localization.Localizations.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.menu.MenuTerminationGroupKey;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.SessionRepository;

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

    private static final String SESSION_ID_PARAM = "sessionId";

    private final SessionRepository sessionRepository;

    private final MenuOrchestrationService menuService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    public ContentSession createSession(BotRole botRole, Consumer<SessionParamsDto> function,
            boolean isSkippingConfirmation) {
        final UserEntity user = botRole.getUser();
        final Bot bot = botRole.getBot();

        sessionRepository.removeUserOrChatRequestSessionsForUserInBot(user.getId(), bot);
        final List<Session> sessions = sessionRepository.findForUserInBot(user.getId(), bot);

        if (sessions.size() > 1) {
            throw new SessionException("User " + user.getId() + " has more then one "
                    + "content session", null);
        } else if (sessions.size() == 1) {
            LOGGER.trace("User " + user.getId() + " already has a session " + sessions.get(0).getId() + ". Removing...");
            sessionRepository.removeContentSessionsForUserInBot(user.getId(), bot);
        }

        LOGGER.trace("Creating new content session for user " + user.getId() + "...");
        final ContentSession session = new ContentSession();

        session.setId(UUID.randomUUID());
        session.setBotRole(botRole);
        session.setTimestamp(LocalDateTime.now());
        session.setFunction(function);
        session.setMessages(new ArrayList<>());
        session.setMenuInitialized(false);
        session.setSkippingConfirmation(isSkippingConfirmation);
        sessionRepository.save(session);
        LOGGER.trace("Session saved.");
        
        return session;
    }

    @Override
    public void removeSessionsForUserInBot(BotRole botRole) {
        sessionRepository.removeForUserInBot(botRole.getUser().getId(), botRole.getBot());
    }

    @Override
    public void removeSessionsWithoutConfirmationForUser(BotRole botRole) {
        sessionRepository.removeSessionsWithoutConfirmationForUserInBot(botRole.getUser().getId(), botRole.getBot());
    }

    @Override
    public void processResponse(BotRole botRole, Session session, Message message) {
        final ContentSession contentSession = (ContentSession)session;
        
        contentSession.getMessages().add(message);
        LOGGER.trace("Adding new message to the confirmation list...");
        if (contentSession.isSkippingConfirmation()) {
            LOGGER.trace("Only one message is expected, no confirmation message will be sent.");
            commit(botRole, session.getId());
        } else if (!contentSession.isMenuInitialized()) {
            LOGGER.trace("Sending confirmation menu...");
            menuService.initiateMenu(botRole, MenuKey.COMMIT_CONTENT,
                    SESSION_ID_PARAM, contentSession.getId().toString(), MenuTerminationGroupKey.COMMIT_CONTENT,
                    session.getId());
            
            contentSession.setMenuInitialized(true);
        }
        LOGGER.trace("Session response of user " + botRole.getUser().getId() + " has been processed.");
    }

    @Override
    public void commit(BotRole botRole, UUID sessionId) {
        final ContentSession session = (ContentSession)getSession(sessionId, botRole);

        LOGGER.trace("Removing sessions for user " +  botRole.getUser().getId() + "...");
        if (!session.isSkippingConfirmation()) {
            menuService.terminateMenuGroup(MenuTerminationGroupKey.COMMIT_CONTENT, session.getId());
        }
        removeSessionsForUserInBot(botRole);
        LOGGER.trace("All sessions have been removed for user. Executing content session "
                + sessionId + "'s function for user " +  botRole.getUser().getId() + "...");
        session.execute(botRole);
        LOGGER.trace("Content session " + sessionId + "'s function has been executed.");
    }

    @Override
    public void resend(BotRole botRole, UUID sessionId) {
        final ContentSession session = (ContentSession)getSession(sessionId, botRole);

        LOGGER.trace("Removing sessions for user " +  botRole.getUser().getId()
                + " and recreating session...");
        if (!session.isSkippingConfirmation()) {
            menuService.terminateMenuGroup(MenuTerminationGroupKey.COMMIT_CONTENT,
                    localizationLoader.localize(Menu.COMMIT_CONTENT_RESEND_TERMINAL_PAGE, botRole), session.getId());
        }
        removeSessionsForUserInBot(botRole);
        createSession(botRole, session.getFunction());
        LOGGER.trace("All sessions have been removed for user and new session has been created. "
                + "Sending resend message...");

        final Localization resendLoc = localizationLoader.localize(
                Localizations.Service.RESEND_CONTENT, botRole);

        clientManager.sendMessage(botRole, SendMessage.builder()
                .chatId(botRole.getUser().getId())
                .text(resendLoc.getData())
                .entities(resendLoc.getEntities())
                .build());
        LOGGER.trace("Resend message has been sent.");
    }

    @Override
    public void cancel(BotRole botRole, UUID sessionId) {
        final ContentSession session = (ContentSession)getSession(sessionId, botRole);

        if (!session.isSkippingConfirmation()) {
            menuService.terminateMenuGroup(MenuTerminationGroupKey.COMMIT_CONTENT,
                    localizationLoader.localize(Menu.COMMIT_CONTENT_CANCEL_TERMINAL_PAGE, botRole), session.getId());
        }
        removeSessionsForUserInBot(botRole);
    }

    private Session getSession(UUID sessionId, BotRole botRole) {
        final Optional<Session> potentialSession = sessionRepository.find(sessionId);

        if (potentialSession.isEmpty()) {
            throw new ActionExpiredException("There is no session with id " + sessionId
                    + ". It might have expired.", localizationLoader.localize(
                    Error.SESSION_EXPIRED, botRole));
        }
        return potentialSession.get();
    }
}
