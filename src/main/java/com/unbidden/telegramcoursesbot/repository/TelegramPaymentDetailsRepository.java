package com.unbidden.telegramcoursesbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unbidden.telegramcoursesbot.model.TelegramPaymentDetails;

public interface TelegramPaymentDetailsRepository extends JpaRepository<TelegramPaymentDetails, Long> {
    long countByBotIdAndRefundedAtIsNotNull(Long botId);
    
    long countByCourseIdAndRefundedAtIsNotNull(Long courseId);
}
