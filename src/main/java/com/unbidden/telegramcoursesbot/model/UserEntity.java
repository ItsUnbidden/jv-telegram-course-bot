package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

import org.hibernate.Hibernate;
import org.telegram.telegrambots.meta.api.objects.User;

@Getter
@Setter
@Entity
@Table(name = "users")
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

    public String getFullUserInfo() { 
        return id + " — " + getFullName() + " — " + languageCode;
    }

    @Override
    public String toString() {
        return "UserEntity(id=" + id + ", firstName=" + firstName + ", lastName=" + lastName + ", username=" + username
                + ", languageCode=" + languageCode + ", isLanguageManuallySet=" + isLanguageManuallySet + ", isBanned="
                + isBanned + ")";
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || !Hibernate.getClass(this).equals(Hibernate.getClass(o))) return false;

        final UserEntity that = (UserEntity)o;

        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(getId());
    }
}
