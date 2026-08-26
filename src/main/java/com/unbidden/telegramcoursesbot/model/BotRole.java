package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

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

    @Column(nullable = false)
    private boolean isReceivingHomework;

    @Column(nullable = false)
    private boolean isDisabled;

    public BotRole() {
        
    }

    public BotRole(Bot bot, UserEntity user, Role role, boolean isReceivingHomework) {
        this.bot = bot;
        this.user = user;
        this.role = role;
        this.isReceivingHomework = isReceivingHomework;
        this.isDisabled = false;
    }

    @Override
    public String toString() {
        return "BotRole(id=" + getId() + ", botId=" + bot.getId() + ", userId=" + user.getId() + ", roleId=" + role.getId()
                + ", isReceivingHomework=" + isReceivingHomework + ")";
    }
}
