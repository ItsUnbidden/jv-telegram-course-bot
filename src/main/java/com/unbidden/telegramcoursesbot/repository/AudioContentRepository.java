package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.AudioContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioContentRepository extends JpaRepository<AudioContent, Long> {

}
