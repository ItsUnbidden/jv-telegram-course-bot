package com.unbidden.telegramcoursesbot.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.unbidden.telegramcoursesbot.model.MenuSnapshot;
import java.util.List;


public interface MenuSnapshotRepository extends JpaRepository<MenuSnapshot, Long> {
    @EntityGraph(attributePaths = {"botRole", "botRole.user", "botRole.bot"})
    List<MenuSnapshot> findByGroup(String group);
}
