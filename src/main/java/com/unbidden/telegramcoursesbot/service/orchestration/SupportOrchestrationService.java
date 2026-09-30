package com.unbidden.telegramcoursesbot.service.orchestration;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto;
import com.unbidden.telegramcoursesbot.dto.internal.SendMessageResultDto.Result;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.menu.MenuTerminationGroupKey;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.SupportReply;
import com.unbidden.telegramcoursesbot.model.SupportRequest;
import com.unbidden.telegramcoursesbot.model.SupportReply.ReplySide;
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

    public List<SupportRequest> getNewRequestsForUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportService.getNewRequestsForUser(botRole);
    }

    public List<SupportRequest> getRequestsWithRepliesForUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportService.getRequestsWithRepliesForUser(botRole);
    }

    public long countNewRequestsForUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportService.countNewRequestsForUser(botRole);
    }

    public long countRequestsWithRepliesForUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportService.countRequestsWithRepliesForUser(botRole);
    }

    public long countRequestsForStaffMember(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportService.countRequestsForStaffMember(botRole);
    }

    public boolean isUserEligibleForSupport(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        if (botRole.getRole().getType() != RoleType.DIRECTOR && botRole.getRole().getType() != RoleType.USER) return false;

        return supportService.isUserEligibleForSupport(botRole);
    }
    
    public SupportRequest createNewSupportRequest(BotRole botRole, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");
        
        LOGGER.info("User " + botRole.getUser().getId() + " is requesting support...");
        final SupportRequest request = supportService.createNewSupportRequest(botRole, messages);
        LOGGER.debug("New support request has been created. Sending support message to the staff member...");

        sendSupportRequest(request.getStaffMemberBotRole(), new Localizations.Service.SupportInfoParams(
                botRole.getUser().getFullName(), request.getTimestamp()), request);
        
        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.SUPPORT_REQUEST_SENT, botRole));
        LOGGER.debug("Message sent.");

        return request;
    }

    public void replyToSupportRequest(BotRole botRole, Long requestId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        LOGGER.info("User " + botRole.getUser().getId() + " is responding to support request "
                + requestId + "...");
        final SupportReply reply = supportService.createNewSupportReply(botRole, requestId, messages);

        LOGGER.debug("New reply from user " + botRole.getUser().getId() + " to request "
                + reply.getRequest().getId() + " has been created. Terminating outdated menus...");
        menuService.terminateMenuGroup(MenuTerminationGroupKey.SUPPORT_REPLY, reply.getRequest().getId());
        LOGGER.debug("Reply menus removed. Sending content...");

        sendSupportReply(reply.getRequest().getUserBotRole(), new Localizations.Service.SupportReplyInfoParams(
                reply.getRequest().getUserBotRole().getUser().getFullName(),
                entityUtil.getLocalizedTitle(reply.getRequest().getUserBotRole(), botRole)), reply);

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader
                .localize(Localizations.Service.SUPPORT_REQUEST_REPLY_SENT, botRole));
        LOGGER.debug("Message sent.");
    }

    public void replyToReply(BotRole botRole, Long requestId, Long replyId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(replyId, "replyId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        LOGGER.info("User " + botRole.getUser().getId() + " is responding to reply " + replyId + "...");

        final SupportReply lastReply = supportService.getLastReplyForRequest(botRole, requestId);
        final SupportReply reply = supportService.createNewSupportReplyToReply(botRole, replyId, messages);

        LOGGER.debug("New reply from user " + botRole.getUser().getId() + " to reply "
                + reply.getId() + " has been created. Terminating outdated menus...");
        menuService.terminateMenuGroup(MenuTerminationGroupKey.SUPPORT_REPLY, reply.getRequest().getId());

        sendSupportReply(lastReply.getUserBotRole(), new Localizations.Service.SupportReplyInfoParams(
                botRole.getUser().getFullName(), entityUtil.getLocalizedTitle(lastReply.getUserBotRole(), botRole)), reply);

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, localizationLoader
                .localize(Localizations.Service.SUPPORT_REPLY_REPLY_SENT, botRole));
        LOGGER.debug("Message sent.");
    }

    public SupportRequest markAsResolved(BotRole botRole, Long requestId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");

        LOGGER.info("User " + botRole.getUser().getId() + " wants to mark request "
                + requestId + " as resolved.");
        final SupportRequest request = supportService.markAsResolved(botRole, requestId);
        
        LOGGER.info("Request " + request.getId() + " is now resolved.");
        LOGGER.debug("Sending notification messages to both parties...");
        clientManager.sendMessage(botRole, localizationLoader
                .localize(Localizations.Service.SUPPORT_REQUEST_RESOLVED, botRole));
        if (botRole.getId().equals(request.getUserBotRole().getId())) {
            clientManager.sendMessage(request.getStaffMemberBotRole(), localizationLoader
                    .localize(Localizations.Service.SUPPORT_REQUEST_RESOLVED_NOTIFICATION, request.getStaffMemberBotRole(),
                    new Localizations.Service.SupportRequestResolvedNotificationParams(botRole.getUser().getFullName(),
                        entityUtil.getLocalizedTitle(request.getStaffMemberBotRole(), botRole))));
        } else {
            clientManager.sendMessage(request.getUserBotRole(), localizationLoader
                    .localize(Localizations.Service.SUPPORT_REQUEST_RESOLVED_NOTIFICATION, request.getUserBotRole(),
                    new Localizations.Service.SupportRequestResolvedNotificationParams(botRole.getUser().getFullName(),
                        entityUtil.getLocalizedTitle(request.getUserBotRole(), botRole))));
        }
        LOGGER.debug("Messages sent.");
        
        menuService.terminateMenuGroup(MenuTerminationGroupKey.SUPPORT_REPLY, request.getId());
       
        return request;
    }

    public SupportRequest transferRequest(BotRole current, Long targetBotRoleId, Long requestId) {
        Assert.notNull(current, "current cannot be null");
        Assert.notNull(targetBotRoleId, "targetBotRoleId cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");

        LOGGER.info("User " + current.getUser().getId() + " wants to transfer support request " + requestId + ".");
        final SupportRequest request = supportService.transferRequest(current, targetBotRoleId, requestId);

        LOGGER.info("Support request " + requestId + " has been transfered to user " + request.getStaffMemberBotRole().getUser().getId()
                + ". Sending confirmations...");
        final List<SupportReply> replies = supportService.getRepliesForRequest(requestId);

        if (replies.isEmpty()) {
            sendSupportRequest(request.getStaffMemberBotRole(), new Localizations.Service.SupportInfoParams(
                    request.getUserBotRole().getUser().getFullName(), request.getTimestamp()), request);
        } else {
            sendSupportReply(request.getStaffMemberBotRole(), new Localizations.Service.SupportReplyInfoParams(
                    request.getUserBotRole().getUser().getFullName(), entityUtil.getLocalizedTitle(request.getUserBotRole(),
                    request.getStaffMemberBotRole())), replies.getLast());
        }
        clientManager.sendMessageAsync(current, localizationLoader.localize(Localizations.Service.SUPPORT_REQUEST_TRANSFER_SUCCESS, current));
        LOGGER.debug("Confirmation messages sent.");
        
        return request;
    }

    public void reassignSupportRequestsForUser(BotRole current, Long targetBotRoleId) {
        Assert.notNull(current, "current cannot be null");
        Assert.notNull(targetBotRoleId, "targetBotRole cannot be null");

        final Map<BotRole, Integer> resultMap = supportService.reassignSupportRequestsForUser(current, targetBotRoleId);
        final BotRole targetRole = entityUtil.getBotRoleById(current, targetBotRoleId);

        LOGGER.debug("Sending notifications about support reassignments...");
        for (final Entry<BotRole, Integer> pair : resultMap.entrySet()) {
            clientManager.sendMessageAsync(pair.getKey(), localizationLoader.localize(
                    Localizations.Service.SUPPORT_REQUESTS_REASSIGNED_NOTIFICATION,pair.getKey(),
                    new Localizations.Service.SupportRequestsReassignedNotificationParams(targetRole.getUser().getFullName(), pair.getValue())));
        }
        LOGGER.debug("Messages sent.");
    }

    public void sendLastReply(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");
        
        final Optional<SupportRequest> requestOpt = supportService.findUnresolvedRequestForUser(botRole);

        if (requestOpt.isEmpty()) {
            throw new ForbiddenOperationException("User does not have any unresolved support "
                    + "requests", localizationLoader.localize(Error.NO_SUPPORT_REQUESTS_AVAILABLE_FOR_USER, botRole));
        }
        final SupportRequest request = requestOpt.get();

        if (request.getReplies().isEmpty()) {
            throw new ForbiddenOperationException("There are no replies in unresolved request "
                    + request.getId() + ".", localizationLoader.localize(Error.NO_SUPPORT_REPLIES_AVAILABLE_FOR_USER, botRole));
        }
        LOGGER.debug("Fetching last support reply for user " + botRole.getUser().getId() + "...");

        if (request.getReplies().getLast().getUserBotRole().getId().equals(botRole.getId())) {
            LOGGER.debug("The last reply was sent by the user. Sending awaiting response notification...");
            final SendMessageResultDto sendMessage = clientManager.sendMessage(botRole,
                    localizationLoader.localize(Localizations.Service.SUPPORT_REPLY_AWAITING_RESPONSE, botRole));
            
            if (sendMessage.getResult() == Result.OK) {
                menuService.initiateMenu(botRole, MenuKey.SUPPORT_REPLY_TO_REPLY, REQUEST_ID_PARAM,
                        request.getId().toString(), sendMessage.getMessage().getMessageId(),
                        MenuTerminationGroupKey.SUPPORT_REPLY, request.getId());
                LOGGER.debug("Message and menu sent.");
            } else {
                LOGGER.error("Failed to send an awaiting reply message to user " + botRole.getUser().getId() + ".");
            }
        } else {
            LOGGER.debug("The last reply was sent by a staff member. Sending the reply...");
            final BotRole otherBotRole = entityUtil.getBotRoleById(botRole, request.getReplies().getLast().getUserBotRole().getId());

            sendSupportReply(botRole, new Localizations.Service.SupportReplyInfoParams(
                    otherBotRole.getUser().getFullName(), entityUtil.getLocalizedTitle(botRole, otherBotRole)),
                    entityUtil.getSupportReplyById(botRole, request.getReplies().getLast().getId()));
            LOGGER.debug("Content sent.");
        }
    } 

    public void sendSupportRequestFeedback(BotRole botRole, Long requestId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");

        final SupportRequest request = entityUtil.getSupportRequestById(botRole, requestId);

        sendSupportRequest(botRole, new Localizations.Service.SupportInfoParams(request.getUserBotRole().getUser().getFullName(),
                request.getTimestamp()), request);
    }

    public void sendLastSupportReplyFeedbackForRequest(BotRole botRole, Long requestId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");

        final SupportReply lastReply = supportService.getLastReplyForRequest(botRole, requestId);

        if (lastReply.getReplySide() == ReplySide.CUSTOMER) {
            sendSupportReply(botRole, new Localizations.Service.SupportReplyInfoParams(lastReply.getUserBotRole().getUser().getFullName(),
                    entityUtil.getLocalizedTitle(botRole, lastReply.getUserBotRole())), lastReply);
        } else {
            clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.SUPPORT_REPLY_ALREADY_ANSWERED_INFO,
                    botRole, new Localizations.Service.SupportReplyAlreadyAnsweredInfoParams(
                        entityUtil.getSupportRequestById(botRole, requestId).getUserBotRole().getUser().getFullName(),
                        lastReply.getUserBotRole().getUser().getFullName(),
                        entityUtil.getLocalizedTitle(botRole, lastReply.getUserBotRole()))));
            final SendMessageResultDto menuMessage = sendReplyContent(botRole, lastReply);

            if (menuMessage.getResult() == Result.OK) {
                menuService.initiateMenu(botRole, MenuKey.SUPPORT_REPLY_TO_REPLY, REQUEST_ID_PARAM,
                        requestId.toString(), menuMessage.getMessage().getMessageId(),
                        MenuTerminationGroupKey.SUPPORT_REPLY, requestId);
            } else {
                LOGGER.error("Failed to send the support reply.");
                // TODO: introduce fallback
            }
        }
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
                    request.getId().toString(), menuMessage.getMessage().getMessageId(),
                    MenuTerminationGroupKey.SUPPORT_REPLY, request.getId());
        } else {
            LOGGER.error("Failed to send the support request.");
            // TODO: introduce fallback
        }
    }

    private void sendSupportReply(BotRole botRole, Localizations.Service.SupportReplyInfoParams params,
            SupportReply reply) {
        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.SUPPORT_REPLY_INFO, botRole, params));
        final SendMessageResultDto menuMessage = sendReplyContent(botRole, reply);

        if (menuMessage.getResult() == Result.OK) {
            menuService.initiateMenu(botRole, MenuKey.SUPPORT_REPLY_TO_REPLY, 0,
                    Map.of(
                        REPLY_ID_PARAM, reply.getId().toString(),
                        REQUEST_ID_PARAM, reply.getRequest().getId().toString()
                    ), menuMessage.getMessage().getMessageId(),
                    MenuTerminationGroupKey.SUPPORT_REPLY, reply.getRequest().getId());
        } else {
            LOGGER.error("Failed to send the support reply.");
            // TODO: introduce fallback
        }
    }

    private SendMessageResultDto sendReplyContent(BotRole botRole, SupportReply reply) {
        final List<SendMessageResultDto> sendContent = contentService.sendContent(botRole, reply.getContent().getId());
        final SendMessageResultDto menuMessage;

        if (sendContent.size() > 1) {
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .localize(Localizations.Service.SUPPORT_REPLY_MEDIA_GROUP_BYPASS, botRole);

            menuMessage = clientManager.sendMessage(botRole, mediaGroupBypassMessageLoc);
        } else {
            menuMessage = sendContent.get(0);
        }

        return menuMessage;
    }
}
