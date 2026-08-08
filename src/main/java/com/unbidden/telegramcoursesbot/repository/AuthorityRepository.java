package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Authority;
import com.unbidden.telegramcoursesbot.model.AuthorityType;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface AuthorityRepository extends JpaRepository<Authority, Long> {
    @NonNull
    List<Authority> findByTypeIn(@NonNull Collection<AuthorityType> types);
}
