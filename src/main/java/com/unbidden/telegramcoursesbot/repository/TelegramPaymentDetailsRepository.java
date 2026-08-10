package com.unbidden.telegramcoursesbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.unbidden.telegramcoursesbot.model.TelegramPaymentDetails;

public interface TelegramPaymentDetailsRepository extends JpaRepository<TelegramPaymentDetails, Long> {
    long countByBotIdAndRefundedAtIsNotNull(Long botId);
    
    long countByCourseIdAndRefundedAtIsNotNull(Long courseId);

    @Query("""
        select sum(pd.totalAmount)
        from TelegramPaymentDetails pd
        where pd.bot.id = :botId and pd.refundedAt = null
    """)
    long getTotalStarsIncomeInBot(Long botId);

    @Query("""
        select sum(pd.totalAmount)
        from TelegramPaymentDetails pd
        where pd.course.id = :courseId and pd.refundedAt = null
    """)
    long getTotalStarsIncomeForCourse(Long courseId);
}
