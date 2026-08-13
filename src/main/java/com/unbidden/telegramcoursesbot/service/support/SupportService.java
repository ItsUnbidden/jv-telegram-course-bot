package com.unbidden.telegramcoursesbot.service.support;

import com.unbidden.telegramcoursesbot.exception.ActionExpiredException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.Bot;
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

    @Transactional
    public SupportRequest createNewSupportRequest(UserEntity user, Bot bot, List<Message> messages, String tag) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        if (!isUserEligibleForSupport(user, bot)) {
            throw new ForbiddenOperationException("User " + user.getId() + " cannot send another "
                    + "support request without resolving the previous one.", localizationLoader
                    .localize(Error.USER_NOT_ELIGIBLE_FOR_SUPPORT, user));
        }

        final SupportRequest supportRequest = new SupportRequest();

        supportRequest.setUser(user);
        supportRequest.setBot(bot);
        supportRequest.setContent(contentService.parseAndPersistContent(user, bot, messages));
        supportRequest.setTimestamp(LocalDateTime.now());
        supportRequest.setTag(tag);
        supportRequest.setResolved(false);

        return supportRequestRepository.save(supportRequest);
    }

    @Transactional
    public SupportReply createNewSupportReply(UserEntity user, Bot bot, Long requestId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final SupportRequest request = entityUtil.getSupportRequestById(user, bot, requestId);
        
        checkSupportMessageAnswered(user, bot, request);
        checkRequestResolved(user, bot, request);

        final SupportReply reply = new SupportReply();

        reply.setBot(bot);
        reply.setReplySide(ReplySide.SUPPORT);
        reply.setRequest(request);
        reply.setTimestamp(LocalDateTime.now());
        reply.setUser(user);
        reply.setContent(contentService.parseAndPersistContent(user, bot, messages));

        request.getReplies().add(supportReplyRepository.save(reply));
        request.setStaffMember(user);

        return reply;
    }

    @Transactional
    public SupportReply createNewSupportReplyToAReply(UserEntity user, Bot bot, Long replyId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(replyId, "replyId cannot be null");
        Assert.notNull(messages, "messages cannot be null");

        final SupportReply reply = entityUtil.getSupportReplyById(user, bot, replyId);
        
        checkSupportMessageAnswered(user, bot, reply);
        checkRequestResolved(user, bot, reply);

        final SupportReply newReply = new SupportReply();

        newReply.setReplySide((reply.getReplySide().equals(ReplySide.CUSTOMER)
                ? ReplySide.SUPPORT : ReplySide.CUSTOMER));
        newReply.setRequest(reply.getRequest());
        newReply.setBot(bot);
        newReply.setTimestamp(LocalDateTime.now());
        newReply.setUser(user);
        newReply.setContent(contentService.parseAndPersistContent(user, bot, messages));
        supportReplyRepository.save(newReply);

        return reply;
    }

    @Transactional(readOnly = true)
    public List<SupportRequest> getUnresolvedRequests(Bot bot, Pageable pageable) {
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(pageable, "pageable cannot be null");

        return supportRequestRepository.findByBotIdAndIsResolvedFalse(bot.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<SupportRequest> getUnresolvedRequestsForUser(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        return supportRequestRepository.findByUserAndBotAndIsResolvedFalse(user, bot);
    }

    @Transactional
    public SupportRequest markAsResolved(UserEntity user, Bot bot, Long requestId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(requestId, "requestId cannot be null");

        final SupportRequest request = entityUtil.getSupportRequestById(user, bot, requestId);

        checkRequestResolved(user, bot, request);

        request.setResolved(true);
        
        return request;
    }

    @Transactional(readOnly = true)
    public boolean isUserEligibleForSupport(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        return supportRequestRepository.countByUserAndBotAndIsResolvedFalse(user, bot) == 0;
    }

    @Transactional(readOnly = true)
    public List<SupportRequest> getUnresolvedRequestsForUserInBot(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        
        return supportRequestRepository.findByUserAndBotAndIsResolvedFalse(user, bot);
    }

    private boolean checkRequestResolved(UserEntity user, Bot bot, SupportMessage message) {
        final SupportRequest request;
        if (message instanceof SupportRequest castRequest) {
            request = castRequest;
        } else {
            request = ((SupportReply)message).getRequest();
        }
        if (request.isResolved()) {
            throw new ActionExpiredException("Request " + request.getId()
                    + " has already been resoved", localizationLoader.localize(
                    Error.SUPPORT_REQUEST_ALREADY_RESOLVED, user));
        }
        return true;
    }

    private boolean checkSupportMessageAnswered(UserEntity user, Bot bot, SupportMessage message) {
        if (message instanceof SupportRequest request) {
            if (request.getStaffMember() != null) {
                throw new ActionExpiredException("This support request has already been "
                        + "answered by user " + request.getStaffMember().getId(),
                        localizationLoader.localize(
                        Error.SUPPORT_REQUEST_ALREADY_ANSWERED, user, new Error.SupportRequestAlreadyAnsweredParams(
                            request.getStaffMember().getFullName(), entityUtil.getLocalizedTitle(user, bot, request.getStaffMember()))));
            }
        } else {
            final SupportReply reply = (SupportReply)message;

            if (reply.getReply() != null) {
                throw new ActionExpiredException("This reply has already been answered",
                        localizationLoader.localize(Error.REPLY_ALREADY_ANSWERED,
                        user));
            }
        }
        return true;
    }
    
    @Transactional(readOnly = true)
    public boolean checkifUserIsStaffMember(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final List<UserEntity> uneligibleUsers = userRepository.findAllStaffMembers(bot.getId());
        
        if (uneligibleUsers.contains(user)) {
            throw new ForbiddenOperationException("User " + user.getId() + " is a part of the "
                    + "staff, they are uneligible for support", localizationLoader
                    .localize(Error.SUPPORT_STAFF_REQUEST, user));
        }
        return true;
    }
}
