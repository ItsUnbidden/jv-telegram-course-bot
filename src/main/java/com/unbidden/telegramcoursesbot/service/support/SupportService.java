package com.unbidden.telegramcoursesbot.service.support;

import com.unbidden.telegramcoursesbot.model.SupportMessage;
import com.unbidden.telegramcoursesbot.model.SupportReply;
import com.unbidden.telegramcoursesbot.model.SupportRequest;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.SupportRequest.SupportType;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

public interface SupportService {
    @NonNull
    SupportRequest createNewSupportRequest(@NonNull UserEntity user, @NonNull SupportType reason,
            @NonNull LocalizedContent content, String tag);

    @NonNull
    SupportReply replyToSupportRequest(@NonNull UserEntity user, @NonNull SupportRequest request,
            @NonNull LocalizedContent content);

    @NonNull
    SupportReply replyToReply(@NonNull UserEntity user, @NonNull SupportReply reply,
            @NonNull LocalizedContent content);

    @NonNull
    List<SupportRequest> getUnresolvedRequests(@NonNull UserEntity user,
            @NonNull Pageable pageable);

    @NonNull
    List<SupportRequest> getUnresolvedRequestsForUser(@NonNull UserEntity user);

    @NonNull
    SupportRequest markAsResolved(@NonNull UserEntity user, @NonNull SupportRequest request);

    @NonNull
    SupportRequest getRequestById(@NonNull Long id, @NonNull UserEntity user);

    @NonNull
    SupportReply getReplyById(@NonNull Long id, @NonNull UserEntity user);

    boolean isUserEligibleForSupport(@NonNull UserEntity user);

    @NonNull
    SupportMessage getLastReplyForUser(@NonNull UserEntity user);

    @NonNull
    SupportMessage getLastMessageForStaffMember(@NonNull UserEntity user);

    boolean checkRequestResolved(@NonNull SupportMessage message, @NonNull UserEntity user);

    boolean checkSupportMessageAnswered(@NonNull SupportMessage message,
            @NonNull UserEntity user);
}
