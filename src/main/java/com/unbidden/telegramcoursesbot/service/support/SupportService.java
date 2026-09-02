package com.unbidden.telegramcoursesbot.service.support;

import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.SupportMessage;
import com.unbidden.telegramcoursesbot.model.SupportReply;
import com.unbidden.telegramcoursesbot.model.SupportRequest;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.SupportReply.ReplySide;
import com.unbidden.telegramcoursesbot.repository.SupportReplyRepository;
import com.unbidden.telegramcoursesbot.repository.SupportRequestRepository;
import com.unbidden.telegramcoursesbot.repository.UserRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class SupportService {
    private final SupportRequestRepository supportRequestRepository;

    private final SupportReplyRepository supportReplyRepository;

    private final UserRepository userRepository;

    private final ContentService contentService;

    private final LocalizationLoader localizationLoader;

    private final EntityUtil entityUtil;

    @Transactional(readOnly = true)
    public List<SupportRequest> getUnresolvedRequests(Bot bot, Pageable pageable) {
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(pageable, "pageable cannot be null");

        return supportRequestRepository.findByBotIdAndIsResolvedFalse(bot.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<SupportRequest> getUnresolvedRequestsForUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportRequestRepository.findByUserIdAndBotIdAndIsResolvedFalse(botRole.getUser().getId(), botRole.getBot().getId());
    }

    @Transactional(readOnly = true)
    public boolean isUserEligibleForSupport(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return supportRequestRepository.countByUserIdAndBotIdAndIsResolvedFalse(botRole.getUser().getId(), botRole.getBot().getId()) == 0;
    }

    @Transactional(readOnly = true)
    public List<SupportRequest> getUnresolvedRequestsForUserInBot(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");
        
        return supportRequestRepository.findByUserIdAndBotIdAndIsResolvedFalse(botRole.getUser().getId(), botRole.getBot().getId());
    }

    @Transactional(readOnly = true)
    public boolean checkifUserIsStaffMember(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        final List<UserEntity> uneligibleUsers = userRepository.findAllStaffMembers(botRole.getBot().getId());
        
        if (uneligibleUsers.contains(botRole.getUser())) {
            throw new ForbiddenOperationException("User " + botRole.getUser().getId() + " is a part of the "
                    + "staff, they are uneligible for support", localizationLoader
                    .localize(Error.SUPPORT_STAFF_REQUEST, botRole));
        }
        return true;
    }

    @Transactional
    public SupportRequest createNewSupportRequest(BotRole botRole, List<Message> messages, String tag) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        if (!isUserEligibleForSupport(botRole)) {
            throw new ForbiddenOperationException("User " + botRole.getUser().getId() + " cannot send another "
                    + "support request without resolving the previous one.", localizationLoader
                    .localize(Error.USER_NOT_ELIGIBLE_FOR_SUPPORT, botRole));
        }

        final SupportRequest supportRequest = new SupportRequest();

        supportRequest.setUser(botRole.getUser());
        supportRequest.setBot(botRole.getBot());
        supportRequest.setContent(contentService.parseAndPersistContent(botRole, messages));
        supportRequest.setTimestamp(LocalDateTime.now());
        supportRequest.setTag(tag);
        supportRequest.setResolved(false);

        return supportRequestRepository.save(supportRequest);
    }

    @Transactional
    public SupportReply createNewSupportReply(BotRole botRole, Long requestId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final SupportRequest request = entityUtil.getSupportRequestById(botRole, requestId);
        
        checkSupportMessageAnswered(botRole, request);
        checkRequestResolved(botRole, request);

        final SupportReply reply = new SupportReply();

        reply.setBot(botRole.getBot());
        reply.setReplySide(ReplySide.SUPPORT);
        reply.setRequest(request);
        reply.setTimestamp(LocalDateTime.now());
        reply.setUser(botRole.getUser());
        reply.setContent(contentService.parseAndPersistContent(botRole, messages));

        request.getReplies().add(supportReplyRepository.save(reply));
        request.setStaffMember(botRole.getUser());

        return reply;
    }

    @Transactional
    public SupportReply createNewSupportReplyToAReply(BotRole botRole, Long replyId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(replyId, "replyId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final SupportReply reply = entityUtil.getSupportReplyById(botRole, replyId);
        
        checkSupportMessageAnswered(botRole, reply);
        checkRequestResolved(botRole, reply);

        final SupportReply newReply = new SupportReply();

        newReply.setReplySide((reply.getReplySide().equals(ReplySide.CUSTOMER)
                ? ReplySide.SUPPORT : ReplySide.CUSTOMER));
        newReply.setRequest(reply.getRequest());
        newReply.setBot(botRole.getBot());
        newReply.setTimestamp(LocalDateTime.now());
        newReply.setUser(botRole.getUser());
        newReply.setContent(contentService.parseAndPersistContent(botRole, messages));
        supportReplyRepository.save(newReply);

        return reply;
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
            if (request.getStaffMember() != null) {
                throw new ActionExpiredException("This support request has already been "
                        + "answered by user " + request.getStaffMember().getId(),
                        localizationLoader.localize(
                        Error.SUPPORT_REQUEST_ALREADY_ANSWERED, botRole, new Error.SupportRequestAlreadyAnsweredParams(
                            request.getStaffMember().getFullName(), entityUtil.getLocalizedTitle(botRole,
                                entityUtil.getActiveBotRole(botRole, request.getStaffMember().getId())))));
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
