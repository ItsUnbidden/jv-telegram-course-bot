package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    private Long id;

    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String languageCode;

    public User() {
        
    }

    public User(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        this.setId(telegramUser.getId());
        this.setFirstName(telegramUser.getFirstName());
        this.setLastName(telegramUser.getLastName());
        this.setUsername(telegramUser.getUserName());
        this.setLanguageCode(telegramUser.getLanguageCode());
    }
}
