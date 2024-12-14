package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Authority;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface AuthorityRepository extends JpaRepository<Authority, Long> {
    @NonNull
    Optional<Authority> findByType(@NonNull AuthorityType type);
}
