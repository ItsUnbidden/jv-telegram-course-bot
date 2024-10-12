package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Lesson;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    @NonNull
    @Query("from Lesson l left join fetch l.course c left join fetch l.structure s left join "
            + "fetch l.homework h left join fetch h.content hc where c.name = :courseName")
    List<Lesson> findByCourseName(@NonNull String courseName);

    @NonNull
    @Query("from Lesson l left join fetch l.course c left join fetch l.structure s left join "
            + "fetch l.homework h left join fetch h.content hc where l.index = :index "
            + "and c.name = :courseName")
    Optional<Lesson> findByIndexAndCourseName(@NonNull Integer index, @NonNull String courseName);

    @NonNull
    @EntityGraph(attributePaths = {"course", "structure", "homework"})
    Optional<Lesson> findById(@NonNull Long id);
}
