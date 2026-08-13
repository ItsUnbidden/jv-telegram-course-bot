package com.unbidden.telegramcoursesbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unbidden.telegramcoursesbot.model.MenuSnapshot;
import java.util.List;


public interface MenuSnapshotRepository extends JpaRepository<MenuSnapshot, Long> {
    List<MenuSnapshot> findByGroup(String group);
}
