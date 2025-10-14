package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface RoleRepository extends JpaRepository<Role, Long> {
    @NonNull
    @EntityGraph(attributePaths = "authorities")
    Optional<Role> findByType(@NonNull RoleType type);
}
