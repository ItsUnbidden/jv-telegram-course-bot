package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;

import org.hibernate.Hibernate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "menu_termination_groups")
public class MenuTerminationGroup extends BaseEntity {
    @Column(nullable = false)
    private String name;

    private String terminalLocalizationName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToMany(cascade = CascadeType.REMOVE)
    @JoinTable(name = "menu_termination_groups_messages",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "message_id"))
    private List<MessageEntity> messages;

    @Override
    public String toString() {
        return "MenuTerminationGroup(id=" + getId() + ", name=" + name + ", terminalLocalizationName=" + terminalLocalizationName
                + ", userId=" + user.getId() + ", messageEntityIds=" + (Hibernate.isInitialized(messages) ? messages.stream().map(m -> m.getId()).toList() : "LAZY") + ")";
    }
}
