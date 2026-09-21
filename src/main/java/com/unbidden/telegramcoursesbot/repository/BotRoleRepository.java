package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.dto.internal.HomeworkReceiverWithCountDto;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BotRoleRepository extends JpaRepository<BotRole, Long> {
    @Override
    @EntityGraph(attributePaths = {"bot", "user", "role"})
    Optional<BotRole> findById(Long id);
    
    @EntityGraph(attributePaths = {"bot", "role", "user", "role.authorities"})
    Optional<BotRole> findByBotIdAndUserId(Long botId, Long userId);

    @EntityGraph(attributePaths = {"bot", "role", "user"})
    Optional<BotRole> findByBotIdAndUserIdAndIsDisabledFalse(Long botId, Long userId);

    @EntityGraph(attributePaths = {"bot", "role", "user", "role.authorities"})
    List<BotRole> findByBotIdAndRoleTypeAndIsDisabledFalse(Long botId, RoleType type);

    @EntityGraph(attributePaths = {"user", "bot"})
    List<BotRole> findByBotIdAndRoleTypeInAndIsDisabledFalse(Long botId, Collection<RoleType> roleTypes, Pageable pageable);
    
    @EntityGraph(attributePaths = {"user", "bot"})
    List<BotRole> findByRoleTypeInAndIsDisabledFalse(Collection<RoleType> roleTypes, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "bot"})
    List<BotRole> findByUserId(Long userId);

    List<BotRole> findByBotId(Long botId);

    @Query("""
        select br
        from BotRole br
        left join fetch br.user u
        left join fetch br.bot b
        left join br.role r
        left join CourseProgress cp on cp.curator = br
        where br.bot.id = :botId and br.isReceivingHomework
            and (r.type = 'DIRECTOR' or r.type = 'CREATOR' or r.type = 'MENTOR')
        group by br
        order by count(cp.id) asc
        limit 1
    """)
    Optional<BotRole> findHomeworkReceiverInBot(Long botId);

    @Query("""
        select new com.unbidden.telegramcoursesbot.dto.internal.HomeworkReceiverWithCountDto(br, count(cp.id))
        from BotRole br
        left join fetch br.user u
        left join fetch br.bot b
        left join br.role r
        left join CourseProgress cp on cp.curator = br
        where br.id <> :excludedBotRoleId and br.bot.id = :botId and br.isReceivingHomework
            and (r.type = 'DIRECTOR' or r.type = 'CREATOR' or r.type = 'MENTOR')
        group by br
        order by count(cp.id) asc
    """)
    List<HomeworkReceiverWithCountDto> findHomeworkReceiversWithCountsInBotAndExcludeUser(Long botId, Long excludedBotRoleId);

    @Query("""
        from BotRole br
        left join fetch br.user u
        left join fetch br.bot b
        left join br.role r
        where br.bot.id = :botId and br.isReceivingHomework
            and (r.type = 'DIRECTOR' or r.type = 'CREATOR' or r.type = 'MENTOR')
    """)
    List<BotRole> findHomeworkReceiversInBot(Long botId);

    @Query("""
        from BotRole br
        left join fetch br.user u
        left join fetch br.bot b
        where br.role.type = 'CREATOR'        
    """)
    List<BotRole> findAllCreatorRoles();

    long countByBotIdAndRoleType(Long botId, RoleType roleType);

    long countByBotIdAndRoleTypeInAndIsDisabledFalse(Long botId, Collection<RoleType> roleTypes);

    long countByRoleTypeInAndIsDisabledFalse(Collection<RoleType> roleTypes);

    @Query("""
        select count(br.id)
        from BotRole br
        left join br.role r
        where br.id <> :excludedBotRoleId and br.bot.id = :botId and br.isReceivingHomework
            and (r.type = 'DIRECTOR' or r.type = 'CREATOR' or r.type = 'MENTOR')
    """)
    long countHomeworkReceiversInBotAndExcludeUser(Long botId, Long excludedBotRoleId);

    boolean existsByBotIdAndUserId(Long botId, Long userId);
}
