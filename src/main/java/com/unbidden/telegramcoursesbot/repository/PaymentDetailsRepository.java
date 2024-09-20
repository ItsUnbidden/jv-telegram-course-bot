package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.PaymentDetails;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Long> {
    @Query("from PaymentDetails pd left join fetch pd.user u left join fetch pd.course c where"
            + " u.id = :userId and c.name = :courseName")
    List<PaymentDetails> findByUserIdAndCourseName(Long userId, String courseName);
}
