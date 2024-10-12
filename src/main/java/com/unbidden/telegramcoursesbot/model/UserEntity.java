package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.lang.NonNull;

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

    private boolean isBot;

    private boolean isBanned;

    private boolean isAdmin;

    private boolean isReceivingHomeworkRequests;

    public UserEntity() {
        
    }

    public UserEntity(Long id) {
        this.id = id;
    }

    @NonNull
    public String getFullName() {
        if (lastName != null && username != null) {
            return firstName + " \"" + username + "\" " + lastName;
        }
        if (lastName != null) {
            return firstName + " " + lastName;
        }
        return firstName + "(" + id + ")";
    }
}
