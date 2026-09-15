package com.unbidden.telegramcoursesbot.menu.configurer;

import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.BackwardButton;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.TransitoryButton;
import com.unbidden.telegramcoursesbot.menu.handler.AddMappingLocalizationButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.BanButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.CreateBotStartMappingButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.CreateBotTermsButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.CreateCreatorInfoButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.GetContentButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.ListAdminsButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.ReceiveHomeworkToggleButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.RemoveMappingLocalizationButtonHandler;
import com.unbidden.telegramcoursesbot.menu.handler.SetRoleButtonHandler;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminActionsMenu implements MenuConfigurer {
    private static final String IS_GIVE_BAN_PARAM = "isGiveBan";
    private static final String IS_BY_ID_PARAM = "isById";
    private static final String MAPPING_ID_PARAM = "mappingId";
    private static final String CONTENT_ID_PARAM = "contentId";

    private final SetRoleButtonHandler setRoleHandler;
    private final ListAdminsButtonHandler listAdminsHandler;
    private final ReceiveHomeworkToggleButtonHandler receiveHomeworkHandler;
    private final BanButtonHandler banHandler;
    private final AddMappingLocalizationButtonHandler addMappingLocalizationHandler;
    private final RemoveMappingLocalizationButtonHandler removeMappingLocalizationHandler;
    private final GetContentButtonHandler getContentHandler;
    private final CreateCreatorInfoButtonHandler createCreatorInfoHandler;
    private final CreateBotStartMappingButtonHandler createBotStartHandler;
    private final CreateBotTermsButtonHandler createBotTermsHandler;

    private final ContentOrchestrationService contentService;

    private final LocalizationLoader loader;

    @Override
    public Menu configure() {
        final Menu adminActionsMenu = new Menu(MenuKey.ADMIN_ACTIONS);

        final Page page1 = new Page(adminActionsMenu);

        page1.setPageIndex(0);
        page1.setColumns(2);
        page1.setLocalizationFunction(p -> loader.localize(Localizations.Menu.ADMIN_ACTIONS_PAGE_0, p.botRole()));
        page1.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();
            final Bot bot = p.botRole().getBot();

            buttons.add(new TransitoryButton(loader.localize(Localizations.Button.ADD_OR_REMOVE_ADMIN, p.botRole()).getData(), 1));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.LIST_ADMINS, p.botRole()).getData(), listAdminsHandler));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.TOGGLE_RECEIVE_HOMEWORK, p.botRole()).getData(), receiveHomeworkHandler));
            buttons.add(new TransitoryButton(loader.localize(Localizations.Button.BAN_OPTIONS, p.botRole()).getData(), 2));

            if (bot.getCreatorInfo() == null) {
                buttons.add(new TerminalButton(loader.localize(Localizations.Button.CREATE_BOT_CREATOR_INFO, p.botRole()).getData(), createCreatorInfoHandler));
            } else {
                buttons.add(new TransitoryButton(loader.localize(Localizations.Button.BOT_CREATOR_INFO_SETTINGS, p.botRole()).getData(), MAPPING_ID_PARAM,
                        bot.getCreatorInfo().getId().toString(), 4));
            }
            if (bot.getStart() == null) {
                buttons.add(new TerminalButton(loader.localize(Localizations.Button.CREATE_BOT_START, p.botRole()).getData(), createBotStartHandler));
            } else {
                buttons.add(new TransitoryButton(loader.localize(Localizations.Button.BOT_START_SETTINGS, p.botRole()).getData(), MAPPING_ID_PARAM,
                        bot.getStart().getId().toString(), 4));
            }
            if (bot.getTerms() == null) {
                buttons.add(new TerminalButton(loader.localize(Localizations.Button.CREATE_BOT_TERMS, p.botRole()).getData(), createBotTermsHandler));
            } else {
                buttons.add(new TransitoryButton(loader.localize(Localizations.Button.BOT_TERMS_SETTINGS, p.botRole()).getData(), MAPPING_ID_PARAM,
                        bot.getTerms().getId().toString(), 4));
            }

            return buttons;
        });

        final Page page2 = new Page(adminActionsMenu);

        page2.setPageIndex(1);
        page2.setColumns(2);
        page2.setLocalizationFunction(p -> loader.localize(Localizations.Menu.ADMIN_ACTIONS_PAGE_1, p.botRole()));
        page2.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.addAll(Arrays.stream(RoleType.values())
                .filter(rt -> !rt.equals(RoleType.DIRECTOR)
                    && !rt.equals(RoleType.CREATOR)
                    && !rt.equals(RoleType.BANNED))
                .map(rt -> new TerminalButton(rt.toString(), rt.toString(), setRoleHandler)).toList());
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));
            
            return buttons;
        });

        final Page page3 = new Page(adminActionsMenu);

        page3.setPageIndex(2);
        page3.setColumns(1);
        page3.setLocalizationFunction(p -> loader.localize(Localizations.Menu.ADMIN_ACTIONS_PAGE_2, p.botRole()));
        page3.setButtonsFunction(p -> List.of(
            new TransitoryButton(loader.localize(Localizations.Button.GIVE_BAN, p.botRole()).getData(), IS_GIVE_BAN_PARAM, String.valueOf(true), 3),
            new TransitoryButton(loader.localize(Localizations.Button.LIFT_BAN, p.botRole()).getData(), IS_GIVE_BAN_PARAM, String.valueOf(false), 3),
            new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData())
        ));

        final Page page4 = new Page(adminActionsMenu);

        page4.setPageIndex(3);
        page4.setColumns(2);
        page4.setLocalizationFunction(p -> loader.localize(Localizations.Menu.ADMIN_ACTIONS_PAGE_3, p.botRole()));
        page4.setButtonsFunction(p -> List.of(
            new TerminalButton(loader.localize(Localizations.Button.BY_ID, p.botRole()).getData(), IS_BY_ID_PARAM, String.valueOf(true), banHandler),
            new TerminalButton(loader.localize(Localizations.Button.CHOOSE_USER, p.botRole()).getData(), IS_BY_ID_PARAM, String.valueOf(false), banHandler),
            new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData())
        ));

        
        final Page page5 = new Page(adminActionsMenu);

        page5.setPageIndex(4);
        page5.setColumns(3);
        page5.setLocalizationFunction(p -> loader.localize(Localizations.Menu.ADMIN_ACTIONS_PAGE_4, p.botRole()));
        page5.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.addAll(contentService.getContentForMapping(Long.parseLong(p.params().get(MAPPING_ID_PARAM))).stream()
                .map(c -> (Button)new TransitoryButton(c.getLanguageCode().toString() + " (" + c.getId() + ")",
                CONTENT_ID_PARAM, c.getId().toString(), 5)).toList());
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.ADD_MAPPING_LOCALIZATION, p.botRole()).getData(), addMappingLocalizationHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        final Page page6 = new Page(adminActionsMenu);

        page6.setPageIndex(5);
        page6.setColumns(3);
        page6.setLocalizationFunction(p -> loader.localize(Localizations.Menu.ADMIN_ACTIONS_PAGE_5, p.botRole()));
        page6.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TransitoryButton(loader.localize(Localizations.Button.REMOVE_MAPPING_LOCALIZATION, p.botRole()).getData(), 6));
            buttons.add(new TerminalButton(loader.localize(Localizations.Button.GET_CONTENT, p.botRole()).getData(), getContentHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        final Page page7 = new Page(adminActionsMenu);

        page7.setPageIndex(6);
        page7.setColumns(2);
        page7.setLocalizationFunction(p -> loader.localize(Localizations.Menu.ADMIN_ACTIONS_PAGE_6, p.botRole()));
        page7.setButtonsFunction(p -> {
            final List<Button> buttons = new ArrayList<>();

            buttons.add(new TerminalButton(loader.localize(Localizations.Service.YES, p.botRole()).getData(), removeMappingLocalizationHandler));
            buttons.add(new BackwardButton(loader.localize(Localizations.Button.BACK, p.botRole()).getData()));

            return buttons;
        });

        adminActionsMenu.setPages(List.of(page1, page2, page3, page4, page5, page6, page7));

        return adminActionsMenu;
    }
}
