package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.SecurityService;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.LeaveReviewCommentButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.UpdateReviewCommentButtonHandler;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewActionsMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_rwA";
    
    private static final String UPDATE_REVIEW_COMMENT = "urc";
    private static final String LEAVE_COMMENT = "lrc";
    private static final String GET_REVIEW_COMMENT = "grc";
    private static final String MARK_REVIEW_AS_READ = "mrr";

    private static final String BUTTON_LEAVE_REVIEW_COMMENT = "button_leave_review_comment";
    private static final String BUTTON_GET_REVIEW_COMMENT = "button_get_review_comment";
    private static final String BUTTON_MARK_REVIEW_AS_READ = "button_mark_review_as_read";
    private static final String BUTTON_UPDATE_COMMENT = "button_update_comment";

    private final LeaveReviewCommentButtonHandler leaveReviewCommentHandler;
    private final UpdateReviewCommentButtonHandler updateReviewCommentHandler;

    private final MenuService menuService;

    private final ReviewService reviewService;

    private final ContentService contentService;

    private final SecurityService securityService;

    private final LocalizationLoader localizationLoader;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(1);
        page.setButtonsFunction((u, p, b) -> {
            final Review review = reviewService.getReviewById(Long.parseLong(p.get(0)), u, b);
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                BUTTON_MARK_REVIEW_AS_READ, u).getData(), MARK_REVIEW_AS_READ, (b1, u1, pa) -> {
                    if (securityService.grantAccess(b, u, AuthorityType.SEE_REVIEWS)) {
                        reviewService.markReviewAsRead(review, u1);
                    }
                }));
            
            if (review.getCommentContent() != null) {
                buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_GET_REVIEW_COMMENT, u).getData(), GET_REVIEW_COMMENT, (b1, u1, pa) -> {
                        if (securityService.grantAccess(b, u, AuthorityType.SEE_REVIEWS)) {
                            contentService.sendContent(review.getCommentContent(), u1, b1);
                        }
                    }));
                if (review.getCommentedBy().getId().equals(u.getId())) {
                    buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                        BUTTON_UPDATE_COMMENT, u).getData(), UPDATE_REVIEW_COMMENT,
                        updateReviewCommentHandler));
                }
            } else {
                buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_LEAVE_REVIEW_COMMENT, u).getData(), LEAVE_COMMENT,
                    leaveReviewCommentHandler));
            }
            return buttons;
        });
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page));
        menu.setInitialParameterPresent(true);
        menu.setAttachedToMessage(true);
        menu.setUpdateAfterTerminalButtonRequired(true);
        menuService.save(menu);
    }
}
