package com.unbidden.telegramcoursesbot.model;

import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@MappedSuperclass
public abstract class SupportMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToOne
    @JoinColumn(name = "content_id", nullable = false)
    private LocalizedContent content;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
