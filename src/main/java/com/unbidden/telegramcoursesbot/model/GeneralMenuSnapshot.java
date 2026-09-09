package com.unbidden.telegramcoursesbot.model;

import com.unbidden.telegramcoursesbot.menu.MenuKey;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@Entity 
@DiscriminatorValue("GENERAL")
public class GeneralMenuSnapshot extends MenuSnapshot {
    @Column(name = "menu_key")
    @Enumerated(EnumType.STRING)
    private MenuKey key;

    private String pageHistory;
}
