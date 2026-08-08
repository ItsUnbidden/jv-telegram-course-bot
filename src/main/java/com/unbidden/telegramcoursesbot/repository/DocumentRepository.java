package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.Document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DocumentRepository extends JpaRepository<Document, String> {
    @Query("""
        select c.documents
        from DocumentContent c
        left join fetch c.documents.thumbnail th
        where c.id = :contentId
    """)
    List<Document> findByContent(Long contentId);
}
