package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import org.springframework.lang.NonNull;

@Getter
@Setter
@Entity
@Table(name = "bot_roles")
public class BotRole extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
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

    @Override
    public String toString() {
        return "BotRole(id=" + getId() + ", botId=" + bot.getId() + ", userId=" + user.getId() + ", roleId=" + role.getId()
                + ", isReceivingHomework=" + isReceivingHomework + ")";
    }
}
