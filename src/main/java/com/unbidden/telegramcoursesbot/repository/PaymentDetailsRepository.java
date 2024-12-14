package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.PaymentDetails;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Long> {
    @NonNull
    @EntityGraph(attributePaths = {"user", "course"})
    List<PaymentDetails> findByUserIdAndCourseName(@NonNull Long userId,
            @NonNull String courseName);

    @NonNull
    @EntityGraph(attributePaths = {"user", "course"})
    List<PaymentDetails> findByUserAndBot(@NonNull UserEntity user, @NonNull Bot bot);

    @NonNull
    List<PaymentDetails> findByCourse(@NonNull Course course, @NonNull Pageable Pageable);
}
