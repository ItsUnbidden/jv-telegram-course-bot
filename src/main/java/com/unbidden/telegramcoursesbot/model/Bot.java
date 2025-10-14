package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Data
@Table(name = "bots")
@SQLDelete(sql = "UPDATE bots SET is_disabled = true WHERE id = ?")
@SQLRestriction("is_disabled = false")
public class Bot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String token;

    private boolean isDisabled;
}
