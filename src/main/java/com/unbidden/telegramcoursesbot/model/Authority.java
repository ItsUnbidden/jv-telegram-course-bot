package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "authorities")
public class Authority extends BaseEntity {
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthorityType type;

    @Override
    public String toString() {
        return "Authority(" + type + ")";
    }
}
