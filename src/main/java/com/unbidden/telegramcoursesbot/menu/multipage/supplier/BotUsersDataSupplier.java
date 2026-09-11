package com.unbidden.telegramcoursesbot.menu.multipage.supplier;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.richblock.RichBlockTableCell;

import com.unbidden.telegramcoursesbot.dto.internal.MultipageListData;
import com.unbidden.telegramcoursesbot.dto.internal.MultipageListTableData;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.UserRepository;
import com.unbidden.telegramcoursesbot.util.TextUtil;


@Component 
public class BotUsersDataSupplier extends AbstractTableDataSupplier {
    private static final int PAGE_SIZE = 10;

    private final UserRepository userRepository;

    private final TextUtil textUtil;

    public BotUsersDataSupplier(LocalizationLoader loader, UserRepository userRepository, TextUtil textUtil) {
        super(loader);
        this.userRepository = userRepository;
        this.textUtil = textUtil;
    }

    @Override
    public MultipageListData fetchData(BotRole botRole, int page, Map<String, String> params) {
        final Page<UserEntity> users = userRepository.findByRoleType(botRole.getBot().getId(),
                RoleType.USER, PageRequest.of(page, PAGE_SIZE));
        final List<List<RichBlockTableCell>> rows = getUserTable(botRole, users.toList());

        rows.add(List.of(getTablePageData(botRole, 6, page, PAGE_SIZE, users.getNumberOfElements(), users.getTotalElements())));
        return new MultipageListTableData(users.getTotalPages(), users.getTotalElements(), PAGE_SIZE,
                users.getNumberOfElements(), textUtil.getRichTextFromLocalization(loader.localize(Localizations.Menu.MULTIPAGE_LIST_BOT_USERS, botRole)), rows);
    }
}
