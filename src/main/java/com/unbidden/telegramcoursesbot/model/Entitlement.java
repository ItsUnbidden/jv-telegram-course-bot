package com.unbidden.telegramcoursesbot.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "entitlements")
public class Entitlement extends BaseEntity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, columnDefinition = "BINARY(32)")
    private byte[] tokenHash;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EntitlementStatus status;

    @Column(nullable = false)
    private LocalDateTime aquiredAt;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CurrencyCode currency;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String externalPaymentId;

    @Column(nullable = false, columnDefinition = "BINARY(32)")
    private byte[] externalPaymentIdHash;

    private LocalDateTime lastRotatedAt;

    @Version
    private Long version;

    @Override
    public String toString() {
        return "Entitlement(id=" + getId() + ", courseId=" + course.getId() + ", status=" + status + ", aquiredAt="
                + aquiredAt + ", price=" + price + ", currency=" + currency + ", lastRotatedAt=" + lastRotatedAt + ")";
    }

    public static enum EntitlementStatus {
        /**
         * Can be claimed by a user.
         */
        UNCLAIMED,

        /**
         * Has been claimed by a user.
         */
        CLAIMED,

        /**
         * Had been revoked before the user could claim it.
         */
        REVOKED,

        /**
         * Has been refunded or revoked if gifted.
         */
        REFUNDED,

        /**
         * Has not been claimed and the course became unavailable.
         */
        UNAPPLICABLE
    }
}
