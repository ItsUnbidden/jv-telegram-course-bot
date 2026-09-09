package com.unbidden.telegramcoursesbot.menu.multipage;

import java.util.List;

import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.dto.internal.MultipageListData;
import com.unbidden.telegramcoursesbot.menu.multipage.converter.MultipageListDataConverter;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class MultipageListDataConverterManager {
    private final List<MultipageListDataConverter> converters;

    public MultipageListDataConverter getConverter(Class<? extends MultipageListData> type) {
        for (final var converter : converters) {
            if (converter.getType().equals(type)) {
                return converter;
            }
        }
        throw new RuntimeException("Multipage list data converter for type " + type.getName() + " does not exist.");
    }
}
