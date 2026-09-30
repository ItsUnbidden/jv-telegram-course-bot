package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.SupportRequest;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    @EntityGraph(attributePaths = {"staffMemberBotRole", "staffMemberBotRole.user", "staffMemberBotRole.bot",
            "userBotRole", "userBotRole.user", "userBotRole.bot", "content"})
    Optional<SupportRequest> findById(Long id);

    List<SupportRequest> findByStaffMemberBotRoleIdAndIsResolvedFalse(Long staffMemberBotRoleId);

    @EntityGraph(attributePaths = {"replies", "replies.userBotRole"})
    Optional<SupportRequest> findByUserBotRoleIdAndIsResolvedFalse(Long userBotRoleId);

    boolean existsByUserBotRoleIdAndIsResolvedFalse(Long userBotRoleId);

    @Query("""
        select rq
        from SupportRequest rq
        left join fetch rq.userBotRole br
        left join fetch br.user u
        left join SupportReply rp on rp.request = rq
        where rq.staffMemberBotRole.id = :botRoleId and not rq.isResolved
        group by rq
        having count(rp.id) < 1
    """)
    List<SupportRequest> findRequestsWithNoRepliesForUser(Long botRoleId);
    
    @Query("""
        select rq
        from SupportRequest rq
        left join fetch rq.userBotRole br
        left join fetch br.user u
        left join SupportReply rp on rp.request = rq
        where rq.staffMemberBotRole.id = :botRoleId and not rq.isResolved
        group by rq
        having count(rp.id) > 0
    """)
    List<SupportRequest> findRequestsWithRepliesForUser(Long botRoleId);

    @Query("""
        select count(rq)
        from SupportRequest rq
        where rq.staffMemberBotRole.id = :botRoleId and not rq.isResolved
            and not exists(
                select 1
                from SupportReply sr
                where sr.request.id = rq.id
            )
    """)
    long countRequestsWithNoRepliesForUser(Long botRoleId);
    
    @Query("""
        select count(rq)
        from SupportRequest rq
        where rq.staffMemberBotRole.id = :botRoleId and not rq.isResolved
            and exists(
                select 1
                from SupportReply sr
                where sr.request.id = rq.id
            )
    """)
    long countRequestsWithRepliesForUser(Long botRoleId);

    long countByStaffMemberBotRoleIdAndIsResolvedFalse(Long staffMemberBotRoleId);
}
