package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "bots")
@SQLDelete(sql = "UPDATE bots SET is_disabled = true WHERE id = ?")
@SQLRestriction("is_disabled = false")
public class Bot extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String token;

    private boolean isDisabled;

    @Override
    public String toString() {
        return "Bot(id=" + getId() + ", isDisabled=" + isDisabled + ")";
    }
}
