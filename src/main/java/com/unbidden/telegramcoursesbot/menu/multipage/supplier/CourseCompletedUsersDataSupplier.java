package com.unbidden.telegramcoursesbot.menu.multipage.supplier;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.richblock.RichBlockTableCell;
import org.telegram.telegrambots.meta.api.objects.richtext.RichText;
import org.telegram.telegrambots.meta.api.objects.richtext.RichTextPlain;

import com.unbidden.telegramcoursesbot.dto.internal.MultipageListData;
import com.unbidden.telegramcoursesbot.dto.internal.MultipageListTableData;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.UserRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

@Component 
public class CourseCompletedUsersDataSupplier extends AbstractTableDataSupplier {
    private static final String COURSE_ID_PARAM = "courseId";
    private static final int PAGE_SIZE = 10;

    private final ContentOrchestrationService contentService;

    private final UserRepository userRepository;

    private final EntityUtil entityUtil;

    public CourseCompletedUsersDataSupplier(LocalizationLoader loader, ContentOrchestrationService contentService,
            UserRepository userRepository, EntityUtil entityUtil) {
        super(loader);
        this.contentService = contentService;
        this.userRepository = userRepository;
        this.entityUtil = entityUtil;
    }

    @Override
    public MultipageListData fetchData(BotRole botRole, int page, Map<String, String> params) {
        final Long courseId = Long.parseLong(params.get(COURSE_ID_PARAM));

        final Page<UserEntity> users = userRepository.findAllCompletedCourse(courseId, PageRequest.of(page, PAGE_SIZE));
        final RichText text = new RichTextPlain(loader.localize(Localizations.Menu.MULTIPAGE_LIST_COURSE_COMPLETED_USERS, botRole,
                new Localizations.Menu.MultipageListCourseCompletedUsersParams(contentService.getLocalizedText(
                    botRole, entityUtil.getCourseTitle(botRole, courseId)))).getData());
        final List<List<RichBlockTableCell>> rows = getUserTable(botRole, users.toList());

        rows.add(List.of(getTablePageData(botRole, 6, page, PAGE_SIZE, users.getNumberOfElements(), users.getTotalElements())));
        return new MultipageListTableData(users.getTotalPages(), users.getTotalElements(), PAGE_SIZE,
                users.getNumberOfElements(), text, rows);
    }
}
