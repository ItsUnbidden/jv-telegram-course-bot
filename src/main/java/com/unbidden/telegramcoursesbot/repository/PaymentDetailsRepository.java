package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.model.PaymentDetails;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Long> {
    
}
