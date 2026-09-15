package com.unbidden.telegramcoursesbot.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.unbidden.telegramcoursesbot.model.MenuSnapshot;
import java.util.List;
import java.util.Optional;


public interface MenuSnapshotRepository<T extends MenuSnapshot> extends JpaRepository<T, Long> {
    @Override 
    @EntityGraph(attributePaths = {"botRole", "botRole.user", "botRole.bot"})
    Optional<T> findById(Long id);

    @EntityGraph(attributePaths = {"botRole", "botRole.user", "botRole.bot"})
    List<MenuSnapshot> findByGroup(String group);

    @EntityGraph(attributePaths = {"botRole", "botRole.user", "botRole.bot"})
    List<MenuSnapshot> findByBotRoleId(Long botRoleId);
}
