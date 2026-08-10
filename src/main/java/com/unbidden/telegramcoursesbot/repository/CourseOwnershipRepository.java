package com.unbidden.telegramcoursesbot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.unbidden.telegramcoursesbot.model.CourseOwnership;
import com.unbidden.telegramcoursesbot.model.CourseOwnership.OwnershipSource;
import com.unbidden.telegramcoursesbot.model.CourseOwnership.OwnershipStatus;

public interface CourseOwnershipRepository extends JpaRepository<CourseOwnership, Long> {
    @EntityGraph(attributePaths = "lastPaymentDetails")
    Optional<CourseOwnership> findByUserIdAndCourseId(Long userId, Long courseId);

    Optional<CourseOwnership> findByUserIdAndCourseIdAndStatus(Long userId, Long courseId, OwnershipStatus status);

    boolean existsByUserIdAndCourseIdAndAndStatus(Long userId, Long courseId, OwnershipStatus status);

    boolean existsByUserIdAndCourseIdAndAndStatusAndSource(Long userId, Long courseId, OwnershipStatus status, OwnershipSource source);

    long countByUserIdAndCourseBotIdAndStatus(Long userId, Long botId, OwnershipStatus status);

    long countByCourseBotIdAndStatus(Long botId, OwnershipStatus status);

    long countByCourseBotIdAndSourceAndStatus(Long botId, OwnershipSource source, OwnershipStatus status);

    long countByCourseIdAndStatus(Long courseId, OwnershipStatus status);

    long countByCourseIdAndSourceAndStatus(Long courseId, OwnershipSource source, OwnershipStatus status);
}
