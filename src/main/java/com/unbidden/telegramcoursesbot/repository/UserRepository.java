package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    @Query("""
        from UserEntity u
        where exists(
            select 1
            from BotRole br
            left join br.role r
            where br.bot.id = :botId and r.type = :type
        )        
    """)
    List<UserEntity> findByRoleType(Long botId, RoleType type);

    @Query("""
        from UserEntity u
        where exists(
            select 1
            from BotRole br
            left join br.role r
            where br.bot.id = :botId and br.isReceivingHomework
                and (r.type = 'DIRECTOR' or r.type = 'CREATOR' or r.type = 'MENTOR')
        )
    """)
    List<UserEntity> findByReceivingHomeworkInBot(Long botId);
}
