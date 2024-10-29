package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalizedContentRepository extends JpaRepository<LocalizedContent, Long> {

}
