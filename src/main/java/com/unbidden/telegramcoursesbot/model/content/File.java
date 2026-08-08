package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

import org.hibernate.Hibernate;
import org.springframework.lang.NonNull;

@Getter
@Setter
@ToString
@MappedSuperclass
public abstract class File {
    @Id
    private String uniqueId;

    @Column(nullable = false)
    private String id;

    private Long fileSize;

    public File() {
        
    }

    public File(@NonNull String id, @NonNull String uniqueId, Long fileSize) {
        this.id = id;
        this.uniqueId = uniqueId;
        this.fileSize = fileSize;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || !Hibernate.getClass(this).equals(Hibernate.getClass(o))) return false;

        final File that = (File)o;

        return getUniqueId() != null && getUniqueId().equals(that.getUniqueId());
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(getUniqueId());
    }
}
