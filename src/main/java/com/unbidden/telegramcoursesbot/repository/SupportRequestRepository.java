package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.SupportRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"staffMember", "user", "content"})
    Optional<SupportRequest> findById(@NonNull Long id);

    @NonNull
    @Query("from SupportRequest sr left join fetch sr.staffMember sm "
            + "left join fetch sr.user u left join fetch sr.content c "
            + "where sr.isResolved = false")
    List<SupportRequest> findUnresolved(@NonNull Pageable pageable);

    @NonNull
    @Query("from SupportRequest sr left join fetch sr.user u "
            + "where u.id = :userId and sr.isResolved = false")
    List<SupportRequest> findUnresolvedByUser(@NonNull Long userId);

    @NonNull
    @Query("from SupportRequest sr left join fetch sr.staffMember sm "
            + "where sm.id = :userId and sr.isResolved = false")
    List<SupportRequest> findUnresolvedByStaffMember(@NonNull Long userId);
}
