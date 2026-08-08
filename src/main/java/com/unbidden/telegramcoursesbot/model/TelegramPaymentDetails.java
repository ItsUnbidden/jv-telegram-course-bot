package com.unbidden.telegramcoursesbot.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("TELEGRAM")
public class TelegramPaymentDetails extends PaymentDetails {
    @Column(columnDefinition = "TEXT")
    private String telegramPaymentChargeId;

    private LocalDateTime refundedAt;

    public TelegramPaymentDetails() {
        setCurrency(CurrencyCode.XTR);
    }

    @Override
    public String toString() {
        return "TelegramPaymentDetails(id=" + getId() + ", userId=" + getUser().getId() + ", botId=" + getBot().getId()
                + ", courseId=" + getCourse().getId() + ", totalAmount=" + getTotalAmount()
                + ", timestamp=" + getTimestamp() + ", telegramPaymentChargeId=" + telegramPaymentChargeId
                + ", refundedAt=" + refundedAt + ", version=" + getVersion() + ")";
    }
}
