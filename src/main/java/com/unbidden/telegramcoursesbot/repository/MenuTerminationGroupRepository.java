package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.MenuTerminationGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface MenuTerminationGroupRepository
        extends JpaRepository<MenuTerminationGroup, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"user", "messages", "messages.user"})
    Optional<MenuTerminationGroup> findByUserIdAndName(@NonNull Long userId, @NonNull String name);
}
