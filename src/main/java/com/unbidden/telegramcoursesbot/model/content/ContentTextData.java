package com.unbidden.telegramcoursesbot.model.content;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import java.util.List;
import lombok.Data;
import org.springframework.lang.NonNull;

@Data
@Embeddable
public class ContentTextData {
    private String data;

    private boolean isLocalization;

    @OneToMany
    @JoinTable(name = "content_markers", joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "marker_id"))
    private List<MarkerArea> entities;

    public ContentTextData() {

    }

    public ContentTextData(@NonNull String data, @NonNull List<MarkerArea> markers,
            boolean isLocalization) {
        this.data = data;
        this.entities = markers;
        this.isLocalization = isLocalization;
    }
}
