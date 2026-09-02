package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.Document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DocumentRepository extends JpaRepository<Document, String> {
    @Query("""
        select distinct d
        from Document d
        left join fetch d.thumbnail th
        where exists(
            select 1
            from DocumentContent dc
            left join dc.documents d2
            where dc.id = :contentId and d2 = d
        )
    """)
    List<Document> findByContent(Long contentId);
}
