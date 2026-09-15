package com.unbidden.telegramcoursesbot.menu.multipage.supplier;

import java.util.ArrayList;
import java.util.List;

import org.telegram.telegrambots.meta.api.objects.richblock.RichBlockTableCell;
import org.telegram.telegrambots.meta.api.objects.richtext.RichTextBold;
import org.telegram.telegrambots.meta.api.objects.richtext.RichTextPlain;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.UserEntity;

public abstract class AbstractTableDataSupplier extends AbstractDataSupplier {
    protected final LocalizationLoader loader;

    public AbstractTableDataSupplier(LocalizationLoader loader) {
        this.loader = loader;
    }

    protected List<List<RichBlockTableCell>> getUserTable(BotRole botRole, List<UserEntity> users) {
        final List<List<RichBlockTableCell>> rows = new ArrayList<>();

        rows.add(List.of(
            RichBlockTableCell.builder().text(new RichTextPlain(loader.localize(Localizations.Service.TITLE_USER_ID, botRole).getData())).isHeader(true).align("center").valign("middle").build(),
            RichBlockTableCell.builder().text(new RichTextPlain(loader.localize(Localizations.Service.TITLE_USER_FIRST_NAME, botRole).getData())).isHeader(true).align("center").valign("middle").build(),
            RichBlockTableCell.builder().text(new RichTextPlain(loader.localize(Localizations.Service.TITLE_USER_LAST_NAME, botRole).getData())).isHeader(true).align("center").valign("middle").build(),
            RichBlockTableCell.builder().text(new RichTextPlain(loader.localize(Localizations.Service.TITLE_USERNAME, botRole).getData())).isHeader(true).align("center").valign("middle").build(),
            RichBlockTableCell.builder().text(new RichTextPlain(loader.localize(Localizations.Service.TITLE_USER_LANGUAGE, botRole).getData())).isHeader(true).align("center").valign("middle").build(),
            RichBlockTableCell.builder().text(new RichTextPlain(loader.localize(Localizations.Service.TITLE_USER_BANNED, botRole).getData())).isHeader(true).align("center").valign("middle").build()
        ));
        final String notAvailable = loader.localize(Localizations.Service.NOT_AVAILABLE, botRole).getData();
        final String yes = loader.localize(Localizations.Service.YES, botRole).getData();
        final String no = loader.localize(Localizations.Service.NO, botRole).getData();
        
        for (final UserEntity user : users) {
            rows.add(List.of(
                RichBlockTableCell.builder().text(new RichTextPlain(user.getId().toString())).align("center").valign("middle").build(),
                RichBlockTableCell.builder().text(new RichTextPlain(user.getFirstName())).align("center").valign("middle").build(),
                RichBlockTableCell.builder().text(new RichTextPlain(user.getLastName() != null ? user.getLastName() : notAvailable)).align("center").valign("middle").build(),
                RichBlockTableCell.builder().text(new RichTextPlain(user.getUsername() != null ? user.getUsername() : notAvailable)).align("center").valign("middle").build(),
                RichBlockTableCell.builder().text(new RichTextPlain(loader.getLanguageName(user.getLanguageCode()))).align("center").valign("middle").build(),
                RichBlockTableCell.builder().text(new RichTextPlain(user.isBanned() ? yes : no)).align("center").valign("middle").build()
            ));
        }

        return rows;
    }

    protected RichBlockTableCell getTablePageData(BotRole botRole, int numberOfColumns, int currentPage,
            int pageSize, int numberOfElements, long totalElements) {
        final int lowestIndex = pageSize * currentPage;
        final int highestIndex = lowestIndex + numberOfElements;
        final RichTextBold richText;

        if (totalElements > 1) {
            richText = RichTextBold.builder().text(new RichTextPlain(loader.localize(Localizations.Menu.MULTIPAGE_LIST_PAGE_DATA, botRole,
                new Localizations.Menu.MultipageListPageDataParams(lowestIndex + 1, highestIndex, totalElements)).getData())).build();
        } else if (totalElements == 1) {
            richText = RichTextBold.builder().text(new RichTextPlain(loader.localize(Localizations.Menu.MULTIPAGE_LIST_PAGE_DATA_SINGLE, botRole).getData())).build();
        } else {
            richText = RichTextBold.builder().text(new RichTextPlain(loader.localize(Localizations.Menu.MULTIPAGE_LIST_PAGE_DATA_EMPTY, botRole).getData())).build();
        }

        return RichBlockTableCell.builder()
            .text(richText)
            .colspan(numberOfColumns)
            .align("right")
            .valign("middle")
            .build();
    }
}
