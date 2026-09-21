package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoleRepository extends JpaRepository<Role, Long> {
    @EntityGraph(attributePaths = "authorities")
    Optional<Role> findByType(RoleType type);
    
    @Query("""
        from Role r
        where exists(
            select 1
            from BotRole br
            where br.user.id = :userId and br.bot.id = :botId and br.role = r
        )
    """)
    Optional<Role> findByUserIdAndBotId(Long userId, Long botId);
}
