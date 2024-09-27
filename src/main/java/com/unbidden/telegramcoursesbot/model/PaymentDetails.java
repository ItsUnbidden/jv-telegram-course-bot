package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Data
@Table(name = "payment_details")
@SQLDelete(sql = "UPDATE payment_details SET is_valid = false WHERE id = ?")
@SQLRestriction("is_valid = true")
public class PaymentDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private CourseModel course;
    
    @Column(nullable = false)
    private Integer totalAmount;
    
    @Column(nullable = false)
    private String telegramPaymentChargeId;

    private boolean isValid;

    private boolean isSuccessful;

    private boolean isGifted;
}
