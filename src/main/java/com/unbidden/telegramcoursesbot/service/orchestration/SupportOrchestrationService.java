package com.unbidden.telegramcoursesbot.service.orchestration;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto.Result;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.menu.MenuTerminationGroupKey;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.SupportMessage;
import com.unbidden.telegramcoursesbot.model.SupportReply;
import com.unbidden.telegramcoursesbot.model.SupportRequest;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.support.SupportService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(SupportOrchestrationService.class);

    private static final String REQUEST_ID_PARAM = "requestId";
    private static final String REPLY_ID_PARAM = "replyId";

    private final SupportService supportService;

    private final MenuOrchestrationService menuService;

    private final ContentOrchestrationService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;
    
    public SupportRequest createNewSupportRequest(BotRole botRole, List<Message> messages, String tag) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(messages, "messages cannot be null");
        
        LOGGER.info("User " + botRole.getUser().getFullName() + " is requesting support...");
        final SupportRequest request = supportService.createNewSupportRequest(botRole, messages, tag);
        LOGGER.debug("New support request has been created. Sending support messages to staff...");

        // TODO: This is a temporary solution. Implement a toggle for whether a person receives support requests or not.
        final List<BotRole> staff = entityUtil.getSupport(botRole.getBot().getId());
        staff.add(entityUtil.getCreator(botRole.getBot().getId()));
        staff.add(entityUtil.getDirectorBotRole(botRole.getBot().getId()));

        for (final BotRole member : staff) {
            sendSupportRequest(member, new Localizations.Service.SupportInfoParams(botRole.getUser().getFullName(),
                    request.getTimestamp(), (tag != null) ? tag : localizationLoader.localize(
                        Localizations.Service.NOT_AVAILABLE, member).getData()), request);
        }

        LOGGER.info("Support request for user " + botRole.getUser().getFullName() +  " has been created.");
        return request;
    }

    public void replyToSupportRequest(BotRole botRole, Long requestId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        LOGGER.info("User " + botRole.getUser().getFullName() + " is responding to support request "
                + requestId + "...");
        final SupportReply reply = supportService.createNewSupportReply(botRole, requestId, messages);

        LOGGER.debug("New reply from user " + botRole.getUser().getFullName() + " to request "
                + reply.getRequest().getId() + " has been created. Terminating outdated menus...");
        menuService.terminateMenuGroup(MenuTerminationGroupKey.SUPPORT_REPLY, reply.getRequest().getId());
        LOGGER.debug("Reply menus removed. Sending content...");
        final BotRole requestUserRole = entityUtil.getActiveBotRole(botRole, reply.getRequest().getUser().getId());

        sendSupportReply(requestUserRole, new Localizations.Service.SupportReplyInfoParams(
                requestUserRole.getUser().getFullName(), entityUtil.getLocalizedTitle(requestUserRole, botRole)), reply);

        LOGGER.info("A new reply " + reply.getId() + " has been created.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader
                .localize(Localizations.Service.SUPPORT_REQUEST_REPLY_SENT, botRole));
        LOGGER.debug("Message sent.");
    }

    public void replyToReply(BotRole botRole, Long replyId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(replyId, "replyId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        LOGGER.info("User " + botRole.getUser().getId() + " is responding to reply " + replyId + "...");

        final SupportReply reply = supportService.createNewSupportReplyToAReply(botRole, replyId, messages);
        final BotRole requestUser = entityUtil.getActiveBotRole(botRole, reply.getRequest().getUser().getId());

        LOGGER.debug("New reply from user " + botRole.getUser().getFullName() + " to reply "
                + reply.getId() + " has been created.");
        sendSupportReply(requestUser, new Localizations.Service.SupportReplyInfoParams(
                requestUser.getUser().getFullName(), entityUtil.getLocalizedTitle(requestUser, botRole)), reply);

        LOGGER.info("A new reply " + reply.getId() + " has been created.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader
                .localize(Localizations.Service.SUPPORT_REPLY_REPLY_SENT, botRole));
        LOGGER.debug("Message sent.");
    }

    public SupportRequest markAsResolved(BotRole botRole, Long requestId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");

        LOGGER.info("User " + botRole.getUser().getFullName() + " wants to mark request "
                + requestId + " as resolved.");
        final SupportRequest request = supportService.markAsResolved(botRole, requestId);
        final BotRole requestUserRole = entityUtil.getActiveBotRole(botRole, request.getUser().getId());

        LOGGER.debug("Sending notification messages to both parties...");

        clientManager.sendMessage(requestUserRole, localizationLoader
                .localize(Localizations.Service.SUPPORT_REQUEST_RESOLVED, requestUserRole,
                new Localizations.Service.SupportRequestResolvedParams(requestUserRole.getUser().getFullName(),
                entityUtil.getLocalizedTitle(requestUserRole, botRole))));
        
        if (request.getStaffMember() != null) {
            clientManager.sendMessage(botRole, localizationLoader
                    .localize(Localizations.Service.SUPPORT_REQUEST_RESOLVED, botRole,
                    new Localizations.Service.SupportRequestResolvedParams(botRole.getUser().getFullName(),
                        entityUtil.getLocalizedTitle(botRole, botRole))));
            LOGGER.debug("Messages sent.");
        } else {
            LOGGER.debug("User " + botRole.getUser().getId() + " resolved their support request "
                    + request.getId() + " prematurely. Staff member is unavailable, "
                    + "so only one message was sent.");
        }
        try {
            menuService.terminateMenuGroup(MenuTerminationGroupKey.SUPPORT_REPLY, request.getId());
            LOGGER.debug("Some reply menus were terminated.");
        } catch (EntityNotFoundException e) {
            LOGGER.debug("No menus to terminate.");
        }
        LOGGER.info("Request " + request.getId() + " is now resolved.");

        return request;
    }

    /**
     * TODO: figure out what the purpose of this is.
     */
    public SupportMessage getLastReplyForUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");
        
        final List<SupportRequest> requests = supportService.getUnresolvedRequestsForUserInBot(botRole);

        if (requests.isEmpty()) {
            throw new ForbiddenOperationException("User does not have any unresolved support "
                    + "requests", localizationLoader.localize(Error.NO_SUPPORT_REQUESTS_AVAILABLE_FOR_USER, botRole));
        } else {
            LOGGER.warn("User " + botRole.getUser().getFullName() + " somehow has more than one unresolved support request. Request IDs: "
                    + requests.stream().map(req -> req.getId()).toList());
        }
        final SupportRequest request = requests.getFirst();

        if (request.getReplies().isEmpty()) {
            throw new ForbiddenOperationException("There are no replies in unresolved request "
                    + request.getId() + ".", localizationLoader.localize(Error.NO_SUPPORT_REQUESTS_AVAILABLE_FOR_USER, botRole)); // TODO: a potentially wrong localization (requests instead of replies)
        }
        LOGGER.debug("Fetching last support message for user " + botRole.getUser().getFullName() + "...");
        final SupportReply lastReply = (request.getReplies().getLast().getUser().getId().equals(botRole.getUser().getId()))
                ? request.getReplies().getLast() : request.getReplies().get(request.getReplies().size() - 2);
        final BotRole replyUserRole = entityUtil.getActiveBotRole(botRole, lastReply.getUser().getId());

        LOGGER.debug("Sending reply content...");
        
        sendSupportReply(botRole, new Localizations.Service.SupportReplyInfoParams(lastReply.getUser().getFullName(),
                entityUtil.getLocalizedTitle(botRole, replyUserRole)), lastReply);
        LOGGER.debug("Content sent.");

        return lastReply;
    }
    
    public boolean checkifUserIsStaffMember(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportService.checkifUserIsStaffMember(botRole);
    }

    public boolean isUserEligibleForSupport(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportService.isUserEligibleForSupport(botRole);
    }

    public List<SupportRequest> getUnresolvedRequestsForUserInBot(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");
        
        return supportService.getUnresolvedRequestsForUserInBot(botRole);
    }

    private void sendSupportRequest(BotRole botRole, Localizations.Service.SupportInfoParams params,
            SupportRequest request) {
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.SUPPORT_INFO, botRole, params));
        final List<SendMessageResultDto> sendContent = contentService.sendContent(botRole, request.getContent().getId());
        final SendMessageResultDto menuMessage;
        
        if (sendContent.size() > 1) {
            final Localization mediaGroupBypassMessageLoc = localizationLoader.localize(
                    Localizations.Service.SUPPORT_REQUEST_MEDIA_GROUP_BYPASS, botRole);

            menuMessage = clientManager.sendMessage(botRole, mediaGroupBypassMessageLoc);
        } else {
            menuMessage = sendContent.get(0);
        }

        if (menuMessage.getResult() == Result.OK) {
            menuService.initiateMenu(botRole, MenuKey.SUPPORT_REPLY, REQUEST_ID_PARAM,
                    request.getId().toString(), menuMessage.getMessage().getMessageId());
        } else {
            LOGGER.error("Failed to send the support request.");
            // TODO: Like so many other things with support, this needs a revamp.
        }
    }

    private void sendSupportReply(BotRole botRole, Localizations.Service.SupportReplyInfoParams params,
            SupportReply reply) {
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.SUPPORT_REPLY_INFO, botRole, params));
        final List<SendMessageResultDto> sendContent = contentService.sendContent(botRole, reply.getContent().getId());
        final SendMessageResultDto menuMessage;

        if (sendContent.size() > 1) {
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .localize(Localizations.Service.SUPPORT_REPLY_MEDIA_GROUP_BYPASS, botRole);

            menuMessage = clientManager.sendMessage(botRole, mediaGroupBypassMessageLoc);
        } else {
            menuMessage = sendContent.get(0);
        }

        if (menuMessage.getResult() == Result.OK) {
            menuService.initiateMenu(botRole, MenuKey.SUPPORT_REPLY_TO_REPLY, 0,
                    Map.of(
                        REPLY_ID_PARAM, reply.getId().toString(),
                        REQUEST_ID_PARAM, reply.getRequest().getId().toString()
                    ), menuMessage.getMessage().getMessageId());
        } else {
            LOGGER.error("Failed to send the support reply.");
            // TODO: Like so many other things with support, this needs a revamp.
        }
    }
}
