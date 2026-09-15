package com.unbidden.telegramcoursesbot.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "menu_snapshots")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type")
public abstract class MenuSnapshot extends BaseEntity {
    @Column(name = "mtg")
    private String group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_role_id", nullable = false)
    private BotRole botRole;

    private Integer messageId;

    @Column(nullable = false)
    private Integer currentPage;

    @CreationTimestamp 
    @Column(nullable = false)
    private Instant createdAt;

    private String parameters;

    @Version
    private Long version;
}
