package com.unbidden.telegramcoursesbot.dto.internal;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public abstract class MultipageListData{
    private final int numberOfPages;

    private final long totalElements;

    private final int pageSize;

    private final int numberOfElements;

    public MultipageListData(int numberOfPages, long totalElements, int pageSize, int numberOfElements) {
        this.numberOfPages = numberOfPages;
        this.totalElements = totalElements;
        this.pageSize = pageSize;
        this.numberOfElements = numberOfElements;
    }
}
