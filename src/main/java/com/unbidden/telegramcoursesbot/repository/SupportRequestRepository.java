package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.SupportRequest;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"staffMember", "user", "content", "bot"})
    Optional<SupportRequest> findById(@NonNull Long id);

    @NonNull
    @Query("from SupportRequest sr left join fetch sr.staffMember sm left join fetch sr.bot b "
            + "left join fetch sr.user u left join fetch sr.content c where b.id = :botId and "
            + "sr.isResolved = false")
    List<SupportRequest> findUnresolved(@NonNull Long botId, @NonNull Pageable pageable);

    @NonNull
    @EntityGraph(attributePaths = {"bot", "user"})
    List<SupportRequest> findByUserAndBotAndIsResolvedFalse(@NonNull UserEntity user, @NonNull Bot bot);

    long countByUserAndBotAndIsResolvedFalse(@NonNull UserEntity user, @NonNull Bot bot);

    @NonNull
    @Query("from SupportRequest sr left join fetch sr.staffMember sm left join fetch sr.bot b "
            + "where sm.id = :userId and b.id = :botId and sr.isResolved = false")
    List<SupportRequest> findUnresolvedByStaffMemberInBot(@NonNull Long userId,
            @NonNull Long botId);
}
