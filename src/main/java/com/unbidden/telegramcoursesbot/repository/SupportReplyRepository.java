package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.SupportReply;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface SupportReplyRepository extends JpaRepository<SupportReply, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"reply", "request", "user", "content", "bot"})
    Optional<SupportReply> findById(@NonNull Long id);

    @NonNull
    @EntityGraph(attributePaths = {"user", "content", "bot"})
    List<SupportReply> findByRequestId(@NonNull Long requestId);
}
