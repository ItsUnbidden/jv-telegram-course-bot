package com.unbidden.telegramcoursesbot.model;

import com.unbidden.telegramcoursesbot.menu.MenuKey;

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
@Table(name = "menu_snapshots")
public class MenuSnapshot extends BaseEntity {
    @Column(nullable = false, name = "menu_key")
    @Enumerated(EnumType.STRING)
    private MenuKey key;

    @Column(name = "mtg")
    private String group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_role_id", nullable = false)
    private BotRole botRole;

    private Integer messageId;

    private String parameters;

    @Column(nullable = false)
    private Integer currentPage;

    @Column(nullable = false)
    private Integer initialPage;

    private String pageHistory;

    @Version
    private Long version;
}
