package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BotRoleRepository extends JpaRepository<BotRole, Long> {
    @EntityGraph(attributePaths = {"bot", "role", "user", "role.authorities"})
    Optional<BotRole> findByBotIdAndUserId(Long botId, Long userId);

    @EntityGraph(attributePaths = {"user", "bot"})
    List<BotRole> findByUserId(Long userId);

    List<BotRole> findByBotId(Long botId);

    @Query("""
        from BotRole br
        left join fetch br.user u
        left join fetch br.bot b
        where br.role.type = 'CREATOR'        
    """)
    List<BotRole> findAllCreatorRoles();

    long countByBotIdAndRoleType(Long botId, RoleType roleType);

    boolean existsByBotIdAndUserId(Long botId, Long userId);
}
