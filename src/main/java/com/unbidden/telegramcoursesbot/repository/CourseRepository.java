package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Course;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"lessons"})
    Optional<Course> findByName(@NonNull String name);
}
