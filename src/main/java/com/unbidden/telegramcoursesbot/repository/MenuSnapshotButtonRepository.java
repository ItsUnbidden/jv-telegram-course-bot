package com.unbidden.telegramcoursesbot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.unbidden.telegramcoursesbot.model.MenuSnapshotButton;

public interface MenuSnapshotButtonRepository extends JpaRepository<MenuSnapshotButton, Long> {
    @EntityGraph(attributePaths = {"snapshot"})
    Optional<MenuSnapshotButton> findById(Long id);

    @Modifying
    @Query("""
        delete 
        from MenuSnapshotButton sb
        where sb.snapshot.id = :snapshotId        
    """)
    int deleteAllBySnapshotIdInBatch(Long snapshotId);

    @Modifying
    @Query("""
        delete 
        from MenuSnapshotButton sb
        where sb.snapshot.id in :snapshotIds        
    """)
    int deleteAllBySnapshotIdsInBatch(List<Long> snapshotIds);
}
