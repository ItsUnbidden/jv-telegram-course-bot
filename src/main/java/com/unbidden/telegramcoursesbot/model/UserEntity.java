package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import org.telegram.telegrambots.meta.api.objects.User;

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

    @Column(nullable = false)
    private boolean isAdmin;

    public UserEntity() {
        
    }

    public UserEntity(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.username = user.getUserName();
        this.languageCode = user.getLanguageCode();
        this.isBot = user.getIsBot();
    }
}
