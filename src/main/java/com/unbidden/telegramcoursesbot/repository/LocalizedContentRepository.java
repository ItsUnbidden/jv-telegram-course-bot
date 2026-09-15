package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LocalizedContentRepository extends JpaRepository<LocalizedContent, Long> {
    @Query("""
        select cm.content
        from ContentMapping cm
        where cm.id = :mappingId        
    """)
    List<LocalizedContent> findContentByMappingId(Long mappingId);
}
