package com.unbidden.telegramcoursesbot.dto.internal;

import java.util.List;

import org.telegram.telegrambots.meta.api.objects.richblock.RichBlockTableCell;
import org.telegram.telegrambots.meta.api.objects.richtext.RichText;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class MultipageListTableData extends MultipageListTextData {
    private final List<List<RichBlockTableCell>> rows;

    public MultipageListTableData(int numberOfPages, long totalElements, int pageSize, int numberOfElements,
            RichText richText, List<List<RichBlockTableCell>> rows) {
        super(numberOfPages, totalElements, pageSize, numberOfElements, richText);
        this.rows = rows;
    }
}
