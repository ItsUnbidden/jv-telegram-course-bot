package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.PaymentDetails;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"user", "course"})
    List<PaymentDetails> findByUserIdAndCourseNameAndIsValidTrue(@NonNull Long userId,
            @NonNull String courseName);

    @EntityGraph(attributePaths = {"user", "course"})
    long countByUserIdAndCourseNameAndIsValidTrueAndIsGiftedTrue(@NonNull Long userId,
            @NonNull String courseName);

    @NonNull
    @EntityGraph(attributePaths = {"user", "course"})
    List<PaymentDetails> findByUserAndBot(@NonNull UserEntity user, @NonNull Bot bot);

    @NonNull
    List<PaymentDetails> findByCourse(@NonNull Course course, @NonNull Pageable Pageable);

    @NonNull
    List<PaymentDetails> findByCourseAndIsValidTrue(@NonNull Course course,
            @NonNull Pageable Pageable);

    long countByBotAndIsGiftedFalse(@NonNull Bot bot);

    long countByBotAndRefundedAtIsNotNull(@NonNull Bot bot);

    long countByBotAndIsGiftedTrue(@NonNull Bot bot);

    long countByBotAndIsGiftedTrueAndIsValidFalse(@NonNull Bot bot);

    long countByBotAndIsValidTrue(@NonNull Bot bot);

    long countByCourseAndIsGiftedFalse(@NonNull Course course);

    long countByCourseAndRefundedAtIsNotNull(@NonNull Course course);

    long countByCourseAndIsGiftedTrue(@NonNull Course course);

    long countByCourseAndIsGiftedTrueAndIsValidFalse(@NonNull Course course);

    long countByCourseAndIsValidTrue(@NonNull Course course);

    @Query("select sum(pd.totalAmount) from PaymentDetails pd where pd.bot = :bot "
            + "and pd.timestamp <= :before and pd.isValid = true")
    long getTotalBotIncome(@NonNull Bot bot, @NonNull LocalDateTime before);

    @Query("select sum(pd.totalAmount) from PaymentDetails pd where pd.course = :course "
            + "and pd.timestamp <= :before and pd.isValid = true")
    long getTotalCourseIncome(@NonNull Course course, @NonNull LocalDateTime before);
}
