package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.handler.GetReviewCommentButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.LeaveReviewCommentButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.MarkReviewAsReadButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.UpdateReviewCommentButtonHandler;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewActionsMenu implements MenuConfigurer {
    private static final String REVIEW_ID_PARAM = "reviewId";

    private final LeaveReviewCommentButtonHandler leaveReviewCommentHandler;
    private final UpdateReviewCommentButtonHandler updateReviewCommentHandler;
    private final MarkReviewAsReadButtonHandler markReviewAsReadHandler;
    private final GetReviewCommentButtonHandler getReviewCommentHandler;

    private final LocalizationLoader loader;

    private final EntityUtil entityUtil;

    @Override
    public Menu configure() {
        final Menu menu = new Menu(MenuKey.REVIEW_ACTIONS);

        final Page page = new Page(menu);

        page.setPageIndex(0);
        page.setColumns(1);
        page.setButtonsFunction(p -> {
            final Review review = entityUtil.getReviewById(p.user(), p.bot(), Long.parseLong(p.params().get(REVIEW_ID_PARAM)));
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TerminalButton(loader.localize(Localizations.Button.MARK_REVIEW_AS_READ, p.user()).getData(), markReviewAsReadHandler));
            
            if (review.getCommentContent() != null) {
                buttons.add(new TerminalButton(loader.localize(Localizations.Button.GET_REVIEW_COMMENT, p.user()).getData(),
                        review.getCommentContent().getId().toString(), getReviewCommentHandler));

                if (review.getCommentedBy().getId().equals(p.user().getId())) {
                    buttons.add(new TerminalButton(loader.localize(Localizations.Button.UPDATE_COMMENT, p.user()).getData(), updateReviewCommentHandler));
                }
            } else {
                buttons.add(new TerminalButton(loader.localize(Localizations.Button.LEAVE_REVIEW_COMMENT, p.user()).getData(), leaveReviewCommentHandler));
            }

            return buttons;
        });

        menu.setPages(List.of(page));
        menu.setResetAfterTerminal(true);

        return menu;
    }
}
