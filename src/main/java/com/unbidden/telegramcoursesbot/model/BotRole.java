package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.lang.NonNull;

@Entity
@Data
@Table(name = "bot_roles")
public class BotRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    private boolean isReceivingHomework;

    public BotRole() {
        
    }

    public BotRole(@NonNull Bot bot, @NonNull UserEntity user, @NonNull Role role,
            boolean isReceivingHomework) {
        this.bot = bot;
        this.user = user;
        this.role = role;
        this.isReceivingHomework = isReceivingHomework;
    }
}
