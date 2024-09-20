package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.CourseModel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<CourseModel, Long> {
    Optional<CourseModel> findByName(String name);
}
