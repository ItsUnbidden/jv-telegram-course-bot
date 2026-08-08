package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.SupportRequest;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    @EntityGraph(attributePaths = {"staffMember", "user", "content", "bot"})
    Optional<SupportRequest> findById(Long id);

    List<SupportRequest> findByBotIdAndIsResolvedFalse(Long botId, Pageable pageable);

    @EntityGraph(attributePaths = {"bot", "user", "replies"})
    List<SupportRequest> findByUserAndBotAndIsResolvedFalse(UserEntity user, Bot bot);

    long countByUserAndBotAndIsResolvedFalse(UserEntity user, Bot bot);

    List<SupportRequest> findByStaffMemberIdAndBotIdAndIsResolvedFalse(Long staffMemberId, Long botId);
}
