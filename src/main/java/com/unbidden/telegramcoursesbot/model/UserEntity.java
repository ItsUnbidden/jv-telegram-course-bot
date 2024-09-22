package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class UserEntity {
    @Id
    private Long id;

    @Column(nullable = false)
    private String firstName;
    
    private String lastName;
    
    private String username;
    
    private String languageCode;

    @Column(nullable = false)
    private boolean isBot;

    @Column(nullable = false)
    private boolean isBanned;

    public UserEntity() {
        
    }

    public UserEntity(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        this.setId(telegramUser.getId());
        this.setFirstName(telegramUser.getFirstName());
        this.setLastName(telegramUser.getLastName());
        this.setUsername(telegramUser.getUserName());
        this.setLanguageCode(telegramUser.getLanguageCode());
    }
}
