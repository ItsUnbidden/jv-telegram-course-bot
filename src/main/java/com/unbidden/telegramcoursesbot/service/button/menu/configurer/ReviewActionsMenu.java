package com.unbidden.telegramcoursesbot.service.button.menu.configurer;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.service.button.handler.LeaveReviewCommentButtonHandler;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.service.button.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.review.ReviewService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewActionsMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_rwA";

    private static final String LEAVE_COMMENT = "lrc";
    private static final String GET_REVIEW_COMMENT = "grc";
    private static final String MARK_REVIEW_AS_READ = "mrr";

    private static final String BUTTON_LEAVE_REVIEW_COMMENT = "button_leave_review_comment";
    private static final String BUTTON_GET_REVIEW_COMMENT = "button_get_review_comment";
    private static final String BUTTON_MARK_REVIEW_AS_READ = "button_mark_review_as_read";

    private final LeaveReviewCommentButtonHandler leaveReviewCommentHandler;

    private final MenuService menuService;

    private final ReviewService reviewService;

    private final UserService userService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(1);
        page.setButtonsFunction((u, p) -> {
            final Review review = reviewService.getReviewById(Long.parseLong(p.get(0)));
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                    BUTTON_MARK_REVIEW_AS_READ, u).getData(), MARK_REVIEW_AS_READ, (u1, pa) -> {
                        if (userService.isAdmin(u1)) {
                            reviewService.markReviewAsRead(review, u1);
                        }
                    }));
            
            if (review.getCommentContent() != null) {
                buttons.add(new TerminalButton(localizationLoader.getLocalizationForUser(
                        BUTTON_GET_REVIEW_COMMENT, u).getData(), GET_REVIEW_COMMENT, (u1, pa) -> {
                            if (userService.isAdmin(u1)) {
                                bot.sendContent(review.getCommentContent(), u1);
                            }
                        }));
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
