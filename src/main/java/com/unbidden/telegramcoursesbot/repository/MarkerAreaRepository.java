package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.content.MarkerArea;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface MarkerAreaRepository extends JpaRepository<MarkerArea, Long> {
    List<MarkerArea> findByContentId(Long contentId);
}
