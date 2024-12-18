package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.RoleType;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

public interface BotRoleRepository extends JpaRepository<BotRole, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"bot", "role", "user", "role.authorities"})
    Optional<BotRole> findByBotAndUser(@NonNull Bot bot, @NonNull UserEntity user);

    @NonNull
    @Query("from BotRole br left join fetch br.user u left join fetch br.bot b "
            + "left join fetch br.role r where b.id = :botId and r.type = 1")
    Optional<BotRole> findCreator(@NonNull Long botId);

    @NonNull
    @EntityGraph(attributePaths = "user")
    List<BotRole> findByBotAndRoleType(@NonNull Bot bot, @NonNull RoleType type);

    @NonNull
    @EntityGraph(attributePaths = "user")
    List<BotRole> findByBotAndRoleType(@NonNull Bot bot, @NonNull RoleType type,
            @NonNull Pageable pageable);

    @NonNull
    @EntityGraph(attributePaths = {"user", "bot"})
    List<BotRole> findByRoleType(@NonNull RoleType type, @NonNull Pageable pageable);

    @NonNull
    @Query("from BotRole br left join fetch br.user u left join fetch br.bot b "
            + "left join fetch br.role r where b.id = :botId and br.isReceivingHomework = true "
            + "and (r.type = 0 or r.type = 1 or r.type = 3)")
    List<BotRole> findByHomeworkRecievingInBot(@NonNull Long botId);

    @NonNull
    @EntityGraph(attributePaths = {"user", "bot"})
    List<BotRole> findByUser(@NonNull UserEntity user);
}
