package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.SupportReply;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportReplyRepository extends JpaRepository<SupportReply, Long> {
    @EntityGraph(attributePaths = {"reply", "request", "content", "userBotRole", "userBotRole.user", "userBotRole.bot"})
    Optional<SupportReply> findById(Long id);

    @EntityGraph(attributePaths = {"content", "userBotRole", "userBotRole.user", "userBotRole.bot"})
    List<SupportReply> findByRequestIdOrderByTimestampAsc(Long requestId);

    @EntityGraph(attributePaths = {"content", "userBotRole", "userBotRole.user", "userBotRole.bot", "userBotRole.role"})
    Optional<SupportReply> findFirstByRequestIdOrderByTimestampDesc(Long requestId);
}
