package com.unbidden.telegramcoursesbot.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "course_ownerships")
public class CourseOwnership extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDateTime lastUpdate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_details_id")
    private PaymentDetails lastPaymentDetails;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OwnershipStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OwnershipSource source;

    @Version
    private Long version;

    @Override
    public String toString() {
        return "CourseOwnership(id=" + getId() + ", userId=" + user.getId() + ", courseId=" + course.getId()
                + ", lastUpdate=" + lastUpdate + ", lastPaymentDetailsId=" + (lastPaymentDetails != null ? lastPaymentDetails.getId() : "NULL")
                + ", status=" + status + ", version=" + version + ")";
    }

    public static enum OwnershipStatus {
        ACTIVE,
        REVOKED
    }

    public static enum OwnershipSource {
        TELEGRAM,
        EXTERNAL,
        GIFTED
    }
}
