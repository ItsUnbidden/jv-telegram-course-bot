package com.unbidden.telegramcoursesbot.service.support;

import com.unbidden.telegramcoursesbot.dto.internal.BotRoleWithCountDto;
import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.SupportMessage;
import com.unbidden.telegramcoursesbot.model.SupportReply;
import com.unbidden.telegramcoursesbot.model.SupportRequest;
import com.unbidden.telegramcoursesbot.model.SupportReply.ReplySide;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.repository.SupportReplyRepository;
import com.unbidden.telegramcoursesbot.repository.SupportRequestRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class SupportService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(SupportService.class);

    private final SupportRequestRepository supportRequestRepository;

    private final SupportReplyRepository supportReplyRepository;

    private final BotRoleRepository botRoleRepository;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final EntityUtil entityUtil;

    @Transactional(readOnly = true)
    public Optional<SupportRequest> findUnresolvedRequestForUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportRequestRepository.findByUserBotRoleIdAndIsResolvedFalse(botRole.getId());
    }

    @Transactional(readOnly = true)
    public List<SupportReply> getRepliesForRequest(Long requestId) {
        Assert.notNull(requestId, "requestId cannot be null");

        return supportReplyRepository.findByRequestIdOrderByTimestampAsc(requestId);
    }

    @Transactional(readOnly = true)
    public SupportReply getLastReplyForRequest(BotRole botRole, Long requestId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");

        return supportReplyRepository.findFirstByRequestIdOrderByTimestampDesc(requestId).orElseThrow(() ->
                new EntityNotFoundException("Support request " + requestId + " either does not exist or has no replies.",
                localizationLoader.localize(Localizations.Error.SUPPORT_REPLY_NOT_FOUND, botRole)));
    }

    @Transactional(readOnly = true)
    public List<SupportRequest> getNewRequestsForUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportRequestRepository.findRequestsWithNoRepliesForUser(botRole.getId());
    }

    @Transactional(readOnly = true)
    public List<SupportRequest> getRequestsWithRepliesForUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportRequestRepository.findRequestsWithRepliesForUser(botRole.getId());
    }

    @Transactional(readOnly = true)
    public long countNewRequestsForUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportRequestRepository.countRequestsWithNoRepliesForUser(botRole.getId());
    }

    @Transactional(readOnly = true)
    public long countRequestsWithRepliesForUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportRequestRepository.countRequestsWithRepliesForUser(botRole.getId());
    }

    @Transactional(readOnly = true)
    public long countRequestsForStaffMember(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportRequestRepository.countByStaffMemberBotRoleIdAndIsResolvedFalse(botRole.getId());
    }

    @Transactional(readOnly = true)
    public boolean isUserEligibleForSupport(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return !supportRequestRepository.existsByUserBotRoleIdAndIsResolvedFalse(botRole.getId());
    }

    @Transactional
    public SupportRequest createNewSupportRequest(BotRole botRole, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        if (!isUserEligibleForSupport(botRole)) {
            throw new ForbiddenOperationException("User " + botRole.getUser().getId() + " cannot send another "
                    + "support request without resolving the previous one.", localizationLoader
                    .localize(Error.MORE_THAN_ONE_SUPPORT_REQUEST, botRole));
        }
        final Optional<BotRole> staffOpt = botRoleRepository.findSupportReceiverInBot(botRole.getBot().getId());
        final BotRole staff;

        if (staffOpt.isEmpty()) {
            LOGGER.warn("Bot " + botRole.getBot().getId() + " does not have any members who can "
                    + "receive support requests. The request will be assigned to the creator.");
            staff = entityUtil.getCreator(botRole.getBot().getId());
        } else {
            staff = staffOpt.get();
        }
        
        final SupportRequest supportRequest = new SupportRequest();

        supportRequest.setUserBotRole(botRole);
        supportRequest.setStaffMemberBotRole(staff);
        supportRequest.setContent(contentService.parseAndPersistContent(botRole, messages));
        supportRequest.setTimestamp(LocalDateTime.now());
        supportRequest.setResolved(false);

        return supportRequestRepository.save(supportRequest);
    }

    @Transactional
    public SupportReply createNewSupportReply(BotRole botRole, Long requestId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        final SupportRequest request = entityUtil.getSupportRequestById(botRole, requestId);
        
        checkSupportMessageAnswered(botRole, request);
        checkRequestResolved(botRole, request);

        final SupportReply reply = new SupportReply();

        reply.setReplySide(ReplySide.SUPPORT);
        reply.setRequest(request);
        reply.setTimestamp(LocalDateTime.now());
        reply.setUserBotRole(botRole);
        reply.setContent(contentService.parseAndPersistContent(botRole, messages));

        request.getReplies().add(supportReplyRepository.save(reply));

        return reply;
    }

    @Transactional
    public SupportReply createNewSupportReplyToReply(BotRole botRole, Long replyId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(replyId, "replyId cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        final SupportReply reply = entityUtil.getSupportReplyById(botRole, replyId);
        
        checkSupportMessageAnswered(botRole, reply);
        checkRequestResolved(botRole, reply);

        final SupportReply newReply = new SupportReply();

        newReply.setReplySide((reply.getReplySide().equals(ReplySide.CUSTOMER)
                ? ReplySide.SUPPORT : ReplySide.CUSTOMER));
        newReply.setRequest(reply.getRequest());
        newReply.setTimestamp(LocalDateTime.now());
        newReply.setUserBotRole(botRole);
        newReply.setContent(contentService.parseAndPersistContent(botRole, messages));
        supportReplyRepository.save(newReply);

        reply.setReply(newReply);

        return newReply;
    }

    @Transactional
    public SupportRequest markAsResolved(BotRole botRole, Long requestId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");

        final SupportRequest request = entityUtil.getSupportRequestById(botRole, requestId);

        checkRequestResolved(botRole, request);

        request.setResolved(true);
        
        return request;
    }

    @Transactional
    public SupportRequest transferRequest(BotRole current, Long targetBotRoleId, Long requestId) {
        Assert.notNull(current, "current cannot be null");
        Assert.notNull(targetBotRoleId, "targetBotRoleId cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");

        final BotRole target = entityUtil.getBotRoleById(current, targetBotRoleId);

        if (!target.isReceivingSupport()) {
            throw new ForbiddenOperationException("User " + target.getUser().getId() + " does not receive support requests in bot "
                    + target.getBot().getId() + ".", localizationLoader.localize(Localizations.Error.SUPPORT_TRANSFER_DOES_NOT_RECEIVE_SUPPORT, target));
        }
        if (target.getRole().getType() != RoleType.SUPPORT && target.getRole().getType() != RoleType.CREATOR
                && target.getRole().getType() != RoleType.DIRECTOR) {
            throw new ForbiddenOperationException("User " + target.getUser().getId() + " does not have a support role in bot "
                    + target.getBot().getId() + ".", localizationLoader.localize(Localizations.Error.SUPPORT_TRANSFER_WRONG_ROLE, target));
        }
        final SupportRequest request = entityUtil.getSupportRequestById(current, requestId);

        request.setStaffMemberBotRole(target);

        return request;
    }

    @Transactional
    public Map<BotRole, Integer> reassignSupportRequestsForUser(BotRole current, Long targetBotRole) {
        Assert.notNull(current, "current cannot be null");
        Assert.notNull(targetBotRole, "targetBotRole cannot be null");

        final List<SupportRequest> requests = supportRequestRepository.findByStaffMemberBotRoleIdAndIsResolvedFalse(targetBotRole);
        final Map<BotRole, Integer> resultMap = new HashMap<>(); 

        if (requests.isEmpty()) return resultMap;

        final List<BotRoleWithCountDto> support = botRoleRepository.findSupportReceiversWithCountsInBotAndExcludeUser(
                current.getBot().getId(), targetBotRole);  

        LOGGER.info("Reassigning support requests that were curated by user " + requests.getFirst().getStaffMemberBotRole().getId()
                + " in bot " + current.getBot().getId() + "...");
        if (support.isEmpty()) {
            final BotRole creator = entityUtil.getCreator(current.getBot().getId());

            LOGGER.info("There are currently no users in bot " + current.getBot().getId()
                    + " who are receiving support requests. All requests will be assigned to creator " + creator.getId() + ".");

            requests.forEach(r -> r.setStaffMemberBotRole(creator));
            resultMap.put(creator, requests.size());
        } else {
            support.forEach(s -> resultMap.put(s.getCurator(), 0));
            for (final SupportRequest request : requests) {  
                final BotRoleWithCountDto min = support.stream().min((o1, o2) -> {
                    if (o1.getCount() > o2.getCount()) return 1;
                    if (o1.getCount() < o2.getCount()) return -1;
                    return 0;
                }).get();
    
                request.setStaffMemberBotRole(min.getCurator());
                min.setCount(min.getCount() + 1);
                resultMap.put(min.getCurator(), resultMap.get(min.getCurator()) + 1);
            }
        }

        return resultMap;
    }

    private boolean checkRequestResolved(BotRole botRole, SupportMessage message) {
        final SupportRequest request;
        
        if (message instanceof SupportRequest castRequest) {
            request = castRequest;
        } else {
            request = ((SupportReply)message).getRequest();
        }
        if (request.isResolved()) {
            throw new ActionExpiredException("Request " + request.getId()
                    + " has already been resoved", localizationLoader.localize(
                    Error.SUPPORT_REQUEST_ALREADY_RESOLVED, botRole));
        }
        return true;
    }

    private boolean checkSupportMessageAnswered(BotRole botRole, SupportMessage message) {
        if (message instanceof SupportRequest request) {
            if (!request.getReplies().isEmpty()) {
                throw new ActionExpiredException("This support request has already been "
                        + "answered by user " + request.getStaffMemberBotRole().getUser().getId(),
                        localizationLoader.localize(
                        Error.SUPPORT_REQUEST_ALREADY_ANSWERED, botRole, new Error.SupportRequestAlreadyAnsweredParams(
                            request.getStaffMemberBotRole().getUser().getFullName(), entityUtil.getLocalizedTitle(botRole,
                                request.getStaffMemberBotRole()))));
            }
        } else {
            final SupportReply reply = (SupportReply)message;

            if (reply.getReply() != null) {
                throw new ActionExpiredException("This reply has already been answered",
                        localizationLoader.localize(Error.REPLY_ALREADY_ANSWERED, botRole));
            }
        }
        return true;
    }
}
