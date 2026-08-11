package com.unbidden.telegramcoursesbot.service.orchestration;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.SupportMessage;
import com.unbidden.telegramcoursesbot.model.SupportReply;
import com.unbidden.telegramcoursesbot.model.SupportRequest;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.menu.MenuKey;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.MenuTerminationGroupKey;
import com.unbidden.telegramcoursesbot.service.support.SupportService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(SupportOrchestrationService.class);

    private final SupportService supportService;

    private final MenuService menuService;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;
    
    public SupportRequest createNewSupportRequest(UserEntity user, Bot bot, List<Message> messages, String tag) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(messages, "messages cannot be null");
        
        LOGGER.info("User " + user.getFullName() + " is requesting support...");
        final SupportRequest request = supportService.createNewSupportRequest(user, bot, messages, tag);
        LOGGER.debug("New support request has been created. Sending support messages to staff...");

        // TODO: This is a temporary solution. Implement a toggle for whether a person receives support requests or not.
        final List<UserEntity> staff = entityUtil.getSupport(bot);
        staff.add(entityUtil.getCreator(bot));
        staff.add(entityUtil.getDiretor());

        for (final UserEntity member : staff) {
            sendSupportRequest(member, bot, new Localizations.Service.SupportInfoParams(user.getFullName(),
                    request.getTimestamp(), (tag != null) ? tag : localizationLoader.localize(
                        Localizations.Service.NOT_AVAILABLE, member).getData()), request);
        }

        LOGGER.info("Support request for user " + user.getFullName() +  " has been created.");
        return request;
    }

    public SupportReply replyToSupportRequest(UserEntity user, Bot bot, Long requestId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        LOGGER.info("User " + user.getFullName() + " is responding to support request "
                + requestId + "...");
        final SupportReply reply = supportService.createNewSupportReply(user, bot, requestId, messages);

        LOGGER.debug("New reply from user " + user.getFullName() + " to request "
                + reply.getRequest().getId() + " has been created. Terminating outdated menus...");
        menuService.terminateMenuGroup(reply.getRequest().getUser(), bot,
                MenuTerminationGroupKey.SUPPORT_REPLY, reply.getRequest().getId());
        LOGGER.debug("Reply menus removed. Sending content...");

        sendSupportReply(reply.getRequest().getUser(), bot, new Localizations.Service.SupportReplyInfoParams(
                user.getFullName(), entityUtil.getLocalizedTitle(reply.getRequest().getUser(), bot, user)), reply);

        LOGGER.info("A new reply " + reply.getId() + " has been created.");
        return reply;
    }

    public SupportReply replyToReply(UserEntity user, Bot bot, Long replyId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(replyId, "replyId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        LOGGER.info("User " + user.getId() + " is responding to reply " + replyId + "...");

        final SupportReply reply = supportService.createNewSupportReplyToAReply(user, bot, replyId, messages);
        LOGGER.debug("New reply from user " + user.getFullName() + " to reply "
                + reply.getId() + " has been created.");

        sendSupportReply(reply.getUser(), bot, new Localizations.Service.SupportReplyInfoParams(
                user.getFullName(), entityUtil.getLocalizedTitle(reply.getUser(), bot, user)), reply);

        LOGGER.info("A new reply " + reply.getId() + " has been created.");
        return reply;
    }

    public SupportRequest markAsResolved(UserEntity user, Bot bot, Long requestId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");

        LOGGER.info("User " + user.getFullName() + " wants to mark request "
                + requestId + " as resolved.");
        final SupportRequest request = supportService.markAsResolved(user, bot, requestId);

        LOGGER.debug("Sending notification messages to both parties...");

        clientManager.getClient(bot).sendMessage(request.getUser(), localizationLoader
                .localize(Localizations.Service.SUPPORT_REQUEST_RESOLVED, request.getUser(),
                new Localizations.Service.SupportRequestResolvedParams(user.getFullName(),
                entityUtil.getLocalizedTitle(request.getUser(), bot, user))));
        
        if (request.getStaffMember() != null) {
            clientManager.getClient(bot).sendMessage(request.getStaffMember(), localizationLoader
                    .localize(Localizations.Service.SUPPORT_REQUEST_RESOLVED, request.getStaffMember(),
                    new Localizations.Service.SupportRequestResolvedParams(user.getFullName(),
                        entityUtil.getLocalizedTitle(request.getStaffMember(), bot, user))));
            LOGGER.debug("Messages sent.");
        } else {
            LOGGER.debug("User " + user.getId() + " resolved their support request "
                    + request.getId() + " prematurely. Staff member is unavailable, "
                    + "so only one message was sent.");
        }
        try {
            menuService.terminateMenuGroup(request.getUser(), bot, MenuTerminationGroupKey.SUPPORT_REPLY, request.getId());
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
    public SupportMessage getLastReplyForUser(UserEntity user, Bot bot) {
        final List<SupportRequest> requests = supportService.getUnresolvedRequestsForUserInBot(user, bot);

        if (requests.isEmpty()) {
            throw new ForbiddenOperationException("User does not have any unresolved support "
                    + "requests", localizationLoader.localize(
                    Error.NO_SUPPORT_REQUESTS_AVAILABLE_FOR_USER, user));
        } else {
            LOGGER.warn("User " + user.getFullName() + " somehow has more than one unresolved support request. Request IDs: "
                    + requests.stream().map(req -> req.getId()).toList());
        }
        final SupportRequest request = requests.getFirst();

        if (request.getReplies().isEmpty()) {
            throw new ForbiddenOperationException("There are no replies in "
                    + "unresolved request " + request.getId() + ".",
                    localizationLoader.localize(
                    Error.NO_SUPPORT_REQUESTS_AVAILABLE_FOR_USER, user)); // TODO: a potentially wrong localization (requests instead of replies)
        }
        LOGGER.debug("Fetching last support message for user " + user.getFullName() + "...");
        final SupportReply lastReply = (request.getReplies().getLast().getUser().getId().equals(user.getId()))
                ? request.getReplies().getLast() : request.getReplies().get(request.getReplies().size() - 2);

        LOGGER.debug("Sending reply content...");
        
        sendSupportReply(user, bot, new Localizations.Service.SupportReplyInfoParams(lastReply.getUser().getFullName(),
                entityUtil.getLocalizedTitle(user, bot, lastReply.getUser())), lastReply);
        LOGGER.debug("Content sent.");

        return lastReply;
    }
    
    public boolean checkifUserIsStaffMember(UserEntity user, Bot bot) {
        return supportService.checkifUserIsStaffMember(user, bot);
    }

    private void sendSupportRequest(UserEntity target, Bot bot, Localizations.Service.SupportInfoParams params,
            SupportRequest request) {
        clientManager.getClient(bot).sendMessage(target, localizationLoader
                .localize(Localizations.Service.SUPPORT_INFO, target, params));
        final List<Message> sendContent = contentService.sendContent(target, bot, request.getContent().getId());

        final Message menuMessage;
        if (sendContent.size() > 1) {
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .localize(Localizations.Service.SUPPORT_REQUEST_MEDIA_GROUP_BYPASS, target);
            menuMessage = clientManager.getClient(bot).sendMessage(target,
                    mediaGroupBypassMessageLoc);
        } else {
            menuMessage = sendContent.get(0);
        }

        menuService.initiateMenu(target, bot, MenuKey.SUPPORT_REPLY, request.getId().toString(),
                menuMessage.getMessageId());
        menuService.addToMenuTerminationGroup(request.getUser(), target, bot,
                menuMessage.getMessageId(), MenuTerminationGroupKey.SUPPORT_REPLY, request.getId());
    }

    private void sendSupportReply(UserEntity target, Bot bot, Localizations.Service.SupportReplyInfoParams params,
            SupportReply reply) {
        clientManager.getClient(bot).sendMessage(target, localizationLoader
                .localize(Localizations.Service.SUPPORT_REPLY_INFO, target, params));
        final List<Message> sendContent = contentService.sendContent(target, bot, reply.getContent().getId());

        final Message menuMessage;
        if (sendContent.size() > 1) {
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .localize(Localizations.Service.SUPPORT_REPLY_MEDIA_GROUP_BYPASS, target);
            menuMessage = clientManager.getClient(bot).sendMessage(target,
                    mediaGroupBypassMessageLoc);
        } else {
            menuMessage = sendContent.get(0);
        }

        menuService.initiateMenu(target, bot, MenuKey.SUPPORT_REPLY_TO_REPLY, reply.getId().toString(),
                menuMessage.getMessageId());
    }
}
