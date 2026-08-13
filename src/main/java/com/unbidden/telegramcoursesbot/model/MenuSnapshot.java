package com.unbidden.telegramcoursesbot.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.unbidden.telegramcoursesbot.exception.MenuParamsParseException;
import com.unbidden.telegramcoursesbot.menu.MenuKey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "menu_snapshots")
public class MenuSnapshot extends BaseEntity {
    private static final String ELEMENT_DIVIDER = ":";
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MenuKey key;

    private String group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    private Integer messageId;

    private String parameters;

    @Column(nullable = false)
    private Integer currentPage;

    @Column(nullable = false)
    private Integer initialPage;

    private String pageHistory;

    @Version
    private Long version;

    @Transient
    public Map<String, String> paramsToMap() {
        final Map<String, String> map = new HashMap<>();

        if (this.parameters == null) return map;

        final String[] splitStr = this.parameters.split(ELEMENT_DIVIDER);

        for (int i = 0; i < splitStr.length; i += 2) {
            map.put(splitStr[i], splitStr[i + 1]);
        }

        return map;
    }

    @Transient
    public void parseAndSetParams(Map<String, String> params) {
        if (params.isEmpty()) this.parameters = null;

        final StringBuilder builder = new StringBuilder();

        for (final Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new MenuParamsParseException("Menu parameter keys and values can't be null.");
            }
            if (entry.getKey().contains(ELEMENT_DIVIDER) || entry.getValue().contains(ELEMENT_DIVIDER)) {
                throw new MenuParamsParseException("Menu parameter keys and values can't contain \"" + ELEMENT_DIVIDER + "\".");
            }
            if (entry.getKey().isBlank() || entry.getValue().isBlank()) {
                throw new MenuParamsParseException("Menu parameter keys and values can't be blank.");
            }
            builder.append(entry.getKey()).append(ELEMENT_DIVIDER).append(entry.getValue()).append(ELEMENT_DIVIDER);
        }
        builder.delete(builder.length() - 1, builder.length());
        
        this.parameters = builder.toString();
    }

    @Transient
    public List<Integer> historyToList() {
        final List<Integer> history = new ArrayList<>();

        if (this.pageHistory == null) return history;

        final String[] splitStr = this.pageHistory.split(ELEMENT_DIVIDER);

        for (String string : splitStr) {
            history.add(Integer.parseInt(string));
        }

        return history;
    }

    @Transient
    public void parseAndSetHistory(List<Integer> history) {
        if (history.isEmpty()) this.pageHistory = null;

        final StringBuilder builder = new StringBuilder();

        for (final Integer index : history) {
            if (index == null) {
                throw new MenuParamsParseException("Page history elements can't be null.");
            }
            builder.append(index).append(ELEMENT_DIVIDER);
        }
        builder.delete(builder.length() - 1, builder.length());
        
        this.pageHistory = builder.toString();
    }
}
