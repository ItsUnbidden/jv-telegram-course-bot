package com.unbidden.telegramcoursesbot.menu.multipage.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.richblock.InputRichBlock;
import org.telegram.telegrambots.meta.api.objects.richblock.InputRichBlockParagraph;
import org.telegram.telegrambots.meta.api.objects.richblock.InputRichBlockTable;
import org.telegram.telegrambots.meta.api.objects.richtext.InputRichMessage;

import com.unbidden.telegramcoursesbot.dto.internal.MultipageListData;
import com.unbidden.telegramcoursesbot.dto.internal.MultipageListTableData;
import com.unbidden.telegramcoursesbot.model.BotRole;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class MultipageListTableDataConverter implements MultipageListDataConverter {
    @Override
    public InputRichMessage convert(BotRole botRole, MultipageListData data) {
        final var castData = (MultipageListTableData)data; 
        final List<InputRichBlock> blocks = new ArrayList<>();
        
        if (castData.getRichText() != null) {
            blocks.add(InputRichBlockParagraph.builder().text(castData.getRichText()).build());
        }
        blocks.add(InputRichBlockTable.builder().cells(castData.getRows()).isBordered(true).build());

        return InputRichMessage.builder().blocks(blocks).build();
    }

    @Override
    public Class<? extends MultipageListData> getType() {
        return MultipageListTableData.class;
    }
}
