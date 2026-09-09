package com.unbidden.telegramcoursesbot.dto.internal;

import org.telegram.telegrambots.meta.api.objects.richtext.RichText;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MultipageListTextData extends MultipageListData {
    private final RichText richText;

    public MultipageListTextData(int numberOfPages, long totalElements, int pageSize, int numberOfElements,
            RichText richText) {
        super(numberOfPages, totalElements, pageSize, numberOfElements);
        this.richText = richText;
    }
}
