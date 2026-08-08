package com.unbidden.telegramcoursesbot.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bot_payment_tokens")
public class BotPaymentToken extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentTokenStatus status;

    private LocalDateTime lastRotatedAt;

    @Version
    private Long version;

    @Override
    public String toString() {
        return "BotPaymentToken(id=" + getId() + ", botId=" + bot.getId() + ", createdAt=" + createdAt + ", status="
                + status + ", lastRotatedAt=" + lastRotatedAt + ", version=" + version + ")";
    }

    public static enum PaymentTokenStatus {
        ACTIVE,
        SUSPENDED
    }
}
