package com.unbidden.telegramcoursesbot.service.support;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.model.SupportMessage;
import com.unbidden.telegramcoursesbot.model.SupportReply;
import com.unbidden.telegramcoursesbot.model.SupportRequest;
import com.unbidden.telegramcoursesbot.model.SupportRequest.SupportType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.SupportReply.ReplySide;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.repository.SupportReplyRepository;
import com.unbidden.telegramcoursesbot.repository.SupportRequestRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class SupportServiceImpl implements SupportService {
    private static final Logger LOGGER = LogManager.getLogger(SupportServiceImpl.class);

    private static final String REPLY_MENU = "m_rpl";
    private static final String REPLY_TO_REPLY_MENU = "m_rplToRpl";

    private static final String PARAM_SUPPORT_TYPE = "${supportType}";
    private static final String PARAM_TIMESTAMP = "${timestamp}";
    private static final String PARAM_USER_FULL_NAME = "${userFullName}";
    private static final String PARAM_TAG = "${tag}";

    private static final String SERVICE_SUPPORT_REQUEST_MEDIA_GROUP_BYPASS =
            "service_support_request_media_group_bypass";
    private static final String SERVICE_SUPPORT_REPLY_MEDIA_GROUP_BYPASS =
            "service_support_reply_media_group_bypass";
    private static final String SERVICE_SUPPORT_REPLY_INFO = "service_support_reply_info";
    private static final String SERVICE_SUPPORT_INFO = "service_support_info";
    private static final String SERVICE_SUPPORT_REQUEST_RESOLVED =
            "service_support_request_resolved";

    private static final String ERROR_SUPPORT_REQUEST_ALREADY_ANSWERED =
            "error_support_request_already_answered";
    private static final String ERROR_SUPPORT_REPLY_NOT_FOUND = "error_support_reply_not_found";
    private static final String ERROR_SUPPORT_REQUEST_NOT_FOUND =
            "error_support_request_not_found";
    private static final String ERROR_SUPPORT_REQUEST_ALREADY_RESOLVED =
            "error_support_request_already_resolved";
    private static final String ERROR_REPLY_ALREADY_ANSWERED = "error_reply_already_answered";
    private static final String ERROR_USER_NOT_ELIGIBLE_FOR_SUPPORT =
            "error_user_not_eligible_for_support";
    private static final String ERROR_SUPPORT_STAFF_REQUEST = "error_support_staff_request";
    private static final String ERROR_NO_SUPPORT_REQUESTS_AVAILABLE_FOR_USER =
            "error_no_support_requests_available_for_user";

    private static final String SEND_REPLY_MENU_TERMINATION = "support_request_%s_reply_menus";

    private final SupportRequestRepository supportRequestRepository;

    private final SupportReplyRepository supportReplyRepository;

    private final UserService userService;

    private final ContentService contentService;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    @NonNull
    public SupportRequest createNewSupportRequest(@NonNull UserEntity user,
            @NonNull SupportType reason, @NonNull LocalizedContent content, String tag) {
        LOGGER.info("User " + user.getId() + " is requesting support...");
        final Set<UserEntity> uneligibleUsers = new HashSet<>();
        uneligibleUsers.addAll(userService.getSupport());
        uneligibleUsers.add(userService.getDiretor());
        uneligibleUsers.add(userService.getCreator());
        if (uneligibleUsers.contains(user)) {
            throw new ForbiddenOperationException("User " + user.getId() + " is a part of the "
                    + "staff, they are uneligible for support", localizationLoader
                    .getLocalizationForUser(ERROR_SUPPORT_STAFF_REQUEST, user));
        }
        if (!isUserEligibleForSupport(user)) {
            throw new ForbiddenOperationException("User " + user.getId() + " cannot send another "
                    + "support request without resolving previous one.", localizationLoader
                    .getLocalizationForUser(ERROR_USER_NOT_ELIGIBLE_FOR_SUPPORT, user));
        }
        final SupportRequest supportRequest = new SupportRequest();
        supportRequest.setUser(user);
        supportRequest.setContent(content);
        supportRequest.setTimestamp(LocalDateTime.now());
        supportRequest.setSupportType(reason);
        supportRequest.setTag(tag);
        supportRequest.setResolved(false);
        supportRequestRepository.save(supportRequest);
        LOGGER.debug("New support request created. Sending support messages to staff:");

        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_USER_FULL_NAME, supportRequest.getUser().getFullName());
        parameterMap.put(PARAM_TIMESTAMP, supportRequest.getTimestamp());
        parameterMap.put(PARAM_SUPPORT_TYPE, supportRequest.getSupportType());
        parameterMap.put(PARAM_TAG, (supportRequest.getTag() != null) ? supportRequest.getTag()
                : "Not available");

        LOGGER.debug("Sending support request infos to the staff...");
        final List<UserEntity> supportStaff = userService.getSupport();
        for (UserEntity staff : supportStaff) {
            sendSupportRequest(staff, parameterMap, supportRequest);
        }
        LOGGER.debug("Requests have been sent.");
        switch (reason) {
            case SupportType.COURSE:
            LOGGER.debug("Support request is for course. Sending request to creator...");
                sendSupportRequest(userService.getCreator(), parameterMap, supportRequest);
                break;
            default:
            LOGGER.debug("Support request is for platform. Sending request to director...");
                sendSupportRequest(userService.getDiretor(), parameterMap, supportRequest);
                break;
        }
        LOGGER.info("Support request for user " + user.getId() +  " has been created.");
        return supportRequest;
    }

    @Override
    @NonNull
    public SupportReply replyToSupportRequest(@NonNull UserEntity user,
            @NonNull SupportRequest request, @NonNull LocalizedContent content) {
        LOGGER.info("User " + user.getId() + " is responding to support request "
                + request.getId() + "...");
        checkSupportMessageAnswered(request, user);
        checkRequestResolved(request, user);

        final SupportReply reply = new SupportReply();
        reply.setReplySide(ReplySide.SUPPORT);
        reply.setRequest(request);
        reply.setTimestamp(LocalDateTime.now());
        reply.setUser(user);
        reply.setContent(content);
        supportReplyRepository.save(reply);
        LOGGER.debug("New reply from user " + user.getId() + " to request "
                + request.getId() + " has been persisted.");
        request.setReplies(List.of(reply));
        request.setStaffMember(user);
        supportRequestRepository.save(request);
        LOGGER.debug("Request " + request.getId() + " has been updated to include reply "
                + reply.getId() + " from user " + user.getId()
                + ". Terminating outdated menus...");
        menuService.terminateMenuGroup(request.getUser(), SEND_REPLY_MENU_TERMINATION
                .formatted(request.getId()));
        LOGGER.debug("Reply menus removed. Sending content...");
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_USER_FULL_NAME, user.getFullName());

        sendSupportReply(request.getUser(), parameterMap, reply);
        LOGGER.debug("Content sent.");
        return reply;
    }

    @Override
    @NonNull
    public SupportReply replyToReply(@NonNull UserEntity user, @NonNull SupportReply reply,
            @NonNull LocalizedContent content) {
        LOGGER.info("User " + user.getId() + " is responding to reply " + reply.getId() + "...");
        checkSupportMessageAnswered(reply, user);
        checkRequestResolved(reply, user);

        final SupportReply newReply = new SupportReply();
        newReply.setReplySide((reply.getReplySide().equals(ReplySide.CUSTOMER)
                ? ReplySide.SUPPORT : ReplySide.CUSTOMER));
        newReply.setRequest(reply.getRequest());
        newReply.setTimestamp(LocalDateTime.now());
        newReply.setUser(user);
        newReply.setContent(content);
        supportReplyRepository.save(newReply);
        LOGGER.debug("New reply from user " + user.getId() + " to reply "
                + reply.getId() + " has been persisted.");
        
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_USER_FULL_NAME, user.getFullName());

        sendSupportReply(reply.getUser(), parameterMap, newReply);
        LOGGER.debug("Content sent.");
        return newReply;
    }

    @Override
    @NonNull
    public List<SupportRequest> getUnresolvedRequests(@NonNull UserEntity user,
            @NonNull Pageable pageable) {
        return supportRequestRepository.findUnresolved(pageable);
    }

    @Override
    @NonNull
    public List<SupportRequest> getUnresolvedRequestsForUser(@NonNull UserEntity user) {
        return supportRequestRepository.findUnresolvedByUser(user.getId());
    }

    @Override
    @NonNull
    public SupportRequest markAsResolved(@NonNull UserEntity user,
            @NonNull SupportRequest request) {
        LOGGER.info("User " + user.getId() + " wants to mark request "
                + request.getId() + " as resolved.");
                
        checkRequestResolved(request, user);

        request.setResolved(true);
        supportRequestRepository.save(request);
        LOGGER.info("Request " + request.getId() + " is now resolved.");
        LOGGER.debug("Sending notification messages to both parties...");
        final Localization notification = localizationLoader.getLocalizationForUser(
                SERVICE_SUPPORT_REQUEST_RESOLVED, user, PARAM_USER_FULL_NAME,
                user.getFullName());
        client.sendMessage(request.getUser(), notification);
        if (request.getStaffMember() == null) {
            LOGGER.warn("User " + user.getId() + " resolved their support request "
                    + request.getId() + " prematurely. Staff member is unavailable, "
                    + "so only one message will be sent.");
        } else {
            client.sendMessage(request.getStaffMember(), notification);
            LOGGER.debug("Messages sent.");
        }
        try {
            menuService.terminateMenuGroup(request.getUser(), SEND_REPLY_MENU_TERMINATION
                    .formatted(request.getId()));
            LOGGER.debug("Some reply menus were terminated.");
        } catch (EntityNotFoundException e) {
            LOGGER.debug("No menus to terminate.");
        }
        return request;
    }

    @Override
    @NonNull
    public SupportRequest getRequestById(@NonNull Long id, @NonNull UserEntity user) {
        return supportRequestRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Support request with id " + id + " does not exist",
                localizationLoader.getLocalizationForUser(ERROR_SUPPORT_REQUEST_NOT_FOUND,
                user)));
    }

    @Override
    @NonNull
    public SupportReply getReplyById(@NonNull Long id, @NonNull UserEntity user) {
        return supportReplyRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Support reply with id " + id + " does not exist",
                localizationLoader.getLocalizationForUser(ERROR_SUPPORT_REPLY_NOT_FOUND,
                user)));
    }

    @Override
    public boolean isUserEligibleForSupport(@NonNull UserEntity user) {
        return supportRequestRepository.findUnresolvedByUser(user.getId()).isEmpty();
    }

    /**
     * This is currently under development.
     */
    @Override
    @NonNull
    public SupportMessage getLastReplyForUser(@NonNull UserEntity user) {
        final List<SupportRequest> requests = supportRequestRepository
                .findUnresolvedByUser(user.getId());
        if (requests.isEmpty()) {
            throw new ForbiddenOperationException("User does not have any unresolved support "
                    + "requests", localizationLoader.getLocalizationForUser(
                    ERROR_NO_SUPPORT_REQUESTS_AVAILABLE_FOR_USER, user));
        }
        LOGGER.debug("Fetching last support message for user " + user.getId() + "...");
        final List<SupportReply> replies = supportReplyRepository.findByRequestId(
                requests.get(requests.size() - 1).getId());
        final SupportReply lastReply = (replies.get(replies.size() - 1).getUser().getId()
                .equals(user.getId())) ? replies.get(replies.size() - 1) : replies.get(replies.size() - 2);

        LOGGER.debug("Sending reply content...");
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_USER_FULL_NAME, user.getFullName());

        sendSupportReply(user, parameterMap, lastReply);
        LOGGER.debug("Content sent.");
        return lastReply;
    }

    /**
     * This is currently under development.
     */
    @Override
    @NonNull
    public SupportMessage getLastMessageForStaffMember(@NonNull UserEntity user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLastMessageForStaffMember'");
    }

    @Override
    public boolean checkRequestResolved(@NonNull SupportMessage message,
            @NonNull UserEntity user) {
        final SupportRequest request;
        if (message.getClass().equals(SupportRequest.class)) {
            request = (SupportRequest)message;
        } else {
            request = ((SupportReply)message).getRequest();
        }
        if (request.isResolved()) {
            throw new ActionExpiredException("Request " + request.getId()
                    + " has already been resoved", localizationLoader.getLocalizationForUser(
                    ERROR_SUPPORT_REQUEST_ALREADY_RESOLVED, user));
        }
        return true;
    }

    @Override
    public boolean checkSupportMessageAnswered(@NonNull SupportMessage message,
            @NonNull UserEntity user) {
        if (message.getClass().equals(SupportRequest.class)) {
            final SupportRequest request = (SupportRequest)message;
            if (request.getStaffMember() != null) {
                throw new ActionExpiredException("This support request has already been "
                        + "answered by user " + request.getStaffMember().getId(),
                        localizationLoader.getLocalizationForUser(
                        ERROR_SUPPORT_REQUEST_ALREADY_ANSWERED, user,
                        PARAM_USER_FULL_NAME, request.getStaffMember().getFullName()));
            }
        } else {
            final SupportReply reply = (SupportReply)message;
            if (reply.getReply() != null) {
                throw new ActionExpiredException("This reply has already been answered",
                        localizationLoader.getLocalizationForUser(ERROR_REPLY_ALREADY_ANSWERED,
                        user));
            }
        }
        return true;
    }

    private void sendSupportRequest(UserEntity target, Map<String, Object> parameterMap,
            SupportRequest request) {
        client.sendMessage(target, localizationLoader.getLocalizationForUser(
                SERVICE_SUPPORT_INFO, target, parameterMap));
        final List<Message> sendContent = contentService.sendContent(request.getContent(),
                target);

        final Message menuMessage;
        if (sendContent.size() > 1) {
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .getLocalizationForUser(SERVICE_SUPPORT_REQUEST_MEDIA_GROUP_BYPASS, target);
            menuMessage = client.sendMessage(target, mediaGroupBypassMessageLoc);
        } else {
            menuMessage = sendContent.get(0);
        }
        menuService.initiateMenu(REPLY_MENU, target, request.getId().toString(),
                menuMessage.getMessageId());
        menuService.addToMenuTerminationGroup(request.getUser(), target,
                menuMessage.getMessageId(), SEND_REPLY_MENU_TERMINATION.formatted(
                request.getId()), null);
    }

    private void sendSupportReply(UserEntity target, Map<String, Object> parameterMap,
            SupportReply reply) {
        client.sendMessage(target, localizationLoader.getLocalizationForUser(
                SERVICE_SUPPORT_REPLY_INFO, target, parameterMap));
        final List<Message> sendContent = contentService.sendContent(reply.getContent(), target);

        final Message menuMessage;
        if (sendContent.size() > 1) {
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .getLocalizationForUser(SERVICE_SUPPORT_REPLY_MEDIA_GROUP_BYPASS, target);
            menuMessage = client.sendMessage(target, mediaGroupBypassMessageLoc);
        } else {
            menuMessage = sendContent.get(0);
        }
        menuService.initiateMenu(REPLY_TO_REPLY_MENU, target, reply.getId().toString(),
                menuMessage.getMessageId());
    }
}
