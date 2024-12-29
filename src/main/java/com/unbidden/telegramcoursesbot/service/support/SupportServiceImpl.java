package com.unbidden.telegramcoursesbot.service.support;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.model.Bot;
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
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String PARAM_TITLE = "${title}";
    
    private static final String COURSE_NAME = "course_%s_name";

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
    private static final String ERROR_NO_SUPPORT_REQUESTS_AVAILABLE_FOR_USER =
            "error_no_support_requests_available_for_user";

    private static final String SEND_REPLY_MENU_TERMINATION = "support_request_%s_reply_menus";

    private final SupportRequestRepository supportRequestRepository;

    private final SupportReplyRepository supportReplyRepository;

    private final UserService userService;

    private final ContentService contentService;

    private final MenuService menuService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @NonNull
    public SupportRequest createNewSupportRequest(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull SupportType reason, @NonNull LocalizedContent content, String tag) {
        LOGGER.info("User " + user.getId() + " is requesting support...");
        
        if (!isUserEligibleForSupport(user, bot)) {
            throw new ForbiddenOperationException("User " + user.getId() + " cannot send another "
                    + "support request without resolving previous one.", localizationLoader
                    .getLocalizationForUser(ERROR_USER_NOT_ELIGIBLE_FOR_SUPPORT, user));
        }
        final SupportRequest supportRequest = new SupportRequest();
        supportRequest.setUser(user);
        supportRequest.setBot(bot);
        supportRequest.setContent(content);
        supportRequest.setTimestamp(LocalDateTime.now());
        supportRequest.setSupportType(reason);
        supportRequest.setTag(tag);
        supportRequest.setResolved(false);
        supportRequestRepository.save(supportRequest);
        LOGGER.debug("New support request created. Sending support messages to staff:");

        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_USER_FULL_NAME, supportRequest.getUser().getFullName());
        parameterMap.put(PARAM_TIMESTAMP, supportRequest.getTimestamp()
                .truncatedTo(ChronoUnit.SECONDS));
        parameterMap.put(PARAM_SUPPORT_TYPE, supportRequest.getSupportType());
        parameterMap.put(PARAM_TAG, (supportRequest.getTag() != null) ? localizationLoader
                .getLocalizationForUser(COURSE_NAME.formatted(supportRequest.getTag()),
                supportRequest.getUser()) : "Not available");

        LOGGER.debug("Sending support request infos to the staff...");
        final List<UserEntity> supportStaff = userService.getSupport(bot);
        for (UserEntity staff : supportStaff) {
            sendSupportRequest(staff, bot, parameterMap, supportRequest);
        }
        LOGGER.debug("Requests have been sent.");
        switch (reason) {
            case SupportType.COURSE:
            LOGGER.debug("Support request is for course. Sending request to creator...");
                sendSupportRequest(userService.getCreator(bot), bot,
                        parameterMap, supportRequest);
                break;
            default:
            LOGGER.debug("Support request is for platform. Sending request to director...");
                sendSupportRequest(userService.getDiretor(), bot, parameterMap, supportRequest);
                break;
        }
        LOGGER.info("Support request for user " + user.getId() +  " has been created.");
        return supportRequest;
    }

    @Override
    @NonNull
    public SupportReply replyToSupportRequest(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull SupportRequest request, @NonNull LocalizedContent content) {
        LOGGER.info("User " + user.getId() + " is responding to support request "
                + request.getId() + "...");
        checkSupportMessageAnswered(request, user, bot);
        checkRequestResolved(request, user, bot);

        final SupportReply reply = new SupportReply();
        reply.setBot(bot);
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
        menuService.terminateMenuGroup(request.getUser(), bot, SEND_REPLY_MENU_TERMINATION
                .formatted(request.getId()));
        LOGGER.debug("Reply menus removed. Sending content...");
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_USER_FULL_NAME, user.getFullName());
        parameterMap.put(PARAM_TITLE, userService.getLocalizedTitle(user,
                request.getUser(), bot));

        sendSupportReply(request.getUser(), bot, parameterMap, reply);
        LOGGER.debug("Content sent.");
        return reply;
    }

    @Override
    @NonNull
    public SupportReply replyToReply(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull SupportReply reply, @NonNull LocalizedContent content) {
        LOGGER.info("User " + user.getId() + " is responding to reply " + reply.getId() + "...");
        checkSupportMessageAnswered(reply, user, bot);
        checkRequestResolved(reply, user, bot);

        final SupportReply newReply = new SupportReply();
        newReply.setReplySide((reply.getReplySide().equals(ReplySide.CUSTOMER)
                ? ReplySide.SUPPORT : ReplySide.CUSTOMER));
        newReply.setRequest(reply.getRequest());
        newReply.setBot(bot);
        newReply.setTimestamp(LocalDateTime.now());
        newReply.setUser(user);
        newReply.setContent(content);
        supportReplyRepository.save(newReply);
        LOGGER.debug("New reply from user " + user.getId() + " to reply "
                + reply.getId() + " has been persisted.");
        
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_USER_FULL_NAME, user.getFullName());
        parameterMap.put(PARAM_TITLE, userService.getLocalizedTitle(user, reply.getUser(), bot));

        sendSupportReply(reply.getUser(), bot, parameterMap, newReply);
        LOGGER.debug("Content sent.");
        return newReply;
    }

    @Override
    @NonNull
    public List<SupportRequest> getUnresolvedRequests(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull Pageable pageable) {
        return supportRequestRepository.findUnresolved(bot.getId(), pageable);
    }

    @Override
    @NonNull
    public List<SupportRequest> getUnresolvedRequestsForUser(@NonNull UserEntity user,
            @NonNull Bot bot) {
        return supportRequestRepository.findUnresolvedByUserInBot(user.getId(), bot.getId());
    }

    @Override
    @NonNull
    public SupportRequest markAsResolved(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull SupportRequest request) {
        LOGGER.info("User " + user.getId() + " wants to mark request "
                + request.getId() + " as resolved.");
                
        checkRequestResolved(request, user, bot);

        request.setResolved(true);
        supportRequestRepository.save(request);
        LOGGER.info("Request " + request.getId() + " is now resolved.");
        LOGGER.debug("Sending notification messages to both parties...");
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_USER_FULL_NAME, user.getFullName());

        parameterMap.put(PARAM_TITLE, userService.getLocalizedTitle(user, request.getUser(),
                bot));
        clientManager.getClient(bot).sendMessage(request.getUser(), localizationLoader
                .getLocalizationForUser(SERVICE_SUPPORT_REQUEST_RESOLVED, request.getUser(),
                parameterMap));
        if (request.getStaffMember() == null) {
            LOGGER.warn("User " + user.getId() + " resolved their support request "
                    + request.getId() + " prematurely. Staff member is unavailable, "
                    + "so only one message will be sent.");
        } else {
            parameterMap.put(PARAM_TITLE, userService.getLocalizedTitle(user,
                    request.getStaffMember(), bot));
            clientManager.getClient(bot).sendMessage(request.getStaffMember(), localizationLoader
                    .getLocalizationForUser(SERVICE_SUPPORT_REQUEST_RESOLVED,
                    request.getStaffMember(), parameterMap));
            LOGGER.debug("Messages sent.");
        }
        try {
            menuService.terminateMenuGroup(request.getUser(), bot, SEND_REPLY_MENU_TERMINATION
                    .formatted(request.getId()));
            LOGGER.debug("Some reply menus were terminated.");
        } catch (EntityNotFoundException e) {
            LOGGER.debug("No menus to terminate.");
        }
        return request;
    }

    @Override
    @NonNull
    public SupportRequest getRequestById(@NonNull Long id, @NonNull UserEntity user,
            @NonNull Bot bot) {
        return supportRequestRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Support request with id " + id + " does not exist",
                localizationLoader.getLocalizationForUser(ERROR_SUPPORT_REQUEST_NOT_FOUND,
                user)));
    }

    @Override
    @NonNull
    public SupportReply getReplyById(@NonNull Long id, @NonNull UserEntity user,
            @NonNull Bot bot) {
        return supportReplyRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Support reply with id " + id + " does not exist",
                localizationLoader.getLocalizationForUser(ERROR_SUPPORT_REPLY_NOT_FOUND,
                user)));
    }

    @Override
    public boolean isUserEligibleForSupport(@NonNull UserEntity user, @NonNull Bot bot) {
        return supportRequestRepository.findUnresolvedByUserInBot(user.getId(),
                bot.getId()).isEmpty();
    }

    /**
     * This is currently under development.
     */
    @Override
    @NonNull
    public SupportMessage getLastReplyForUser(@NonNull UserEntity user, @NonNull Bot bot) {
        final List<SupportRequest> requests = supportRequestRepository
                .findUnresolvedByUserInBot(user.getId(), bot.getId());
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

        sendSupportReply(user, bot, parameterMap, lastReply);
        LOGGER.debug("Content sent.");
        return lastReply;
    }

    /**
     * This is currently under development.
     */
    @Override
    @NonNull
    public SupportMessage getLastMessageForStaffMember(@NonNull UserEntity user,
            @NonNull Bot bot) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLastMessageForStaffMember'");
    }

    @Override
    public boolean checkRequestResolved(@NonNull SupportMessage message,
            @NonNull UserEntity user, @NonNull Bot bot) {
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
            @NonNull UserEntity user, @NonNull Bot bot) {
        if (message.getClass().equals(SupportRequest.class)) {
            final SupportRequest request = (SupportRequest)message;

            if (request.getStaffMember() != null) {
                final Map<String, Object> parameterMap = new HashMap<>();
                parameterMap.put(PARAM_TITLE, userService.getLocalizedTitle(
                        request.getStaffMember(), user, bot));
                parameterMap.put(PARAM_USER_FULL_NAME, request.getStaffMember().getFullName());
                
                throw new ActionExpiredException("This support request has already been "
                        + "answered by user " + request.getStaffMember().getId(),
                        localizationLoader.getLocalizationForUser(
                        ERROR_SUPPORT_REQUEST_ALREADY_ANSWERED, user, parameterMap));
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

    private void sendSupportRequest(UserEntity target, Bot bot, Map<String, Object> parameterMap,
            SupportRequest request) {
        clientManager.getClient(bot).sendMessage(target, localizationLoader
                .getLocalizationForUser(SERVICE_SUPPORT_INFO, target, parameterMap));
        final List<Message> sendContent = contentService.sendContent(request.getContent(),
                target, bot);

        final Message menuMessage;
        if (sendContent.size() > 1) {
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .getLocalizationForUser(SERVICE_SUPPORT_REQUEST_MEDIA_GROUP_BYPASS, target);
            menuMessage = clientManager.getClient(bot).sendMessage(target,
                    mediaGroupBypassMessageLoc);
        } else {
            menuMessage = sendContent.get(0);
        }
        menuService.initiateMenu(REPLY_MENU, target, request.getId().toString(),
                menuMessage.getMessageId(), bot);
        menuService.addToMenuTerminationGroup(request.getUser(), target, bot,
                menuMessage.getMessageId(), SEND_REPLY_MENU_TERMINATION.formatted(
                request.getId()), null);
    }

    private void sendSupportReply(UserEntity target, Bot bot, Map<String, Object> parameterMap,
            SupportReply reply) {
        clientManager.getClient(bot).sendMessage(target, localizationLoader
                .getLocalizationForUser(SERVICE_SUPPORT_REPLY_INFO, target, parameterMap));
        final List<Message> sendContent = contentService.sendContent(reply.getContent(),
                target, bot);

        final Message menuMessage;
        if (sendContent.size() > 1) {
            final Localization mediaGroupBypassMessageLoc = localizationLoader
                    .getLocalizationForUser(SERVICE_SUPPORT_REPLY_MEDIA_GROUP_BYPASS, target);
            menuMessage = clientManager.getClient(bot).sendMessage(target,
                    mediaGroupBypassMessageLoc);
        } else {
            menuMessage = sendContent.get(0);
        }
        menuService.initiateMenu(REPLY_TO_REPLY_MENU, target, reply.getId().toString(),
                menuMessage.getMessageId(), bot);
    }
}
