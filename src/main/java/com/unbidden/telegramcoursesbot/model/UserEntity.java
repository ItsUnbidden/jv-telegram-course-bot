package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.lang.NonNull;
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
    
    @Column(nullable = false)
    private String languageCode;
    
    private boolean isLanguageManuallySet;

    private boolean isBanned;

    public UserEntity() {
        
    }

    public UserEntity(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.username = user.getUserName();
        this.languageCode = user.getLanguageCode();
        this.isLanguageManuallySet = false;
        this.isBanned = false;
    }

    @NonNull
    public String getFullName() {
        if (lastName != null && username != null) {
            return firstName + " @" + username + " " + lastName;
        }
        if (lastName != null) {
            return firstName + " " + lastName;
        }
        if (username != null) {
            return firstName + " @" + username;
        }
        return firstName + "(" + id + ")";
    }
}
