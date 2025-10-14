package com.unbidden.telegramcoursesbot.service.menu.configurer;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.SecurityService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuConfigurer;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.service.menu.Menu.Page.TerminalButton;
import com.unbidden.telegramcoursesbot.service.menu.handler.GetContentButtonHandler;
import com.unbidden.telegramcoursesbot.service.menu.handler.UploadContentButtonHandler;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentMenu implements MenuConfigurer {
    private static final String MENU_NAME = "m_cntAct";
    private static final String MAPPING_MENU_NAME = "m_mpgOpt";
    
    private static final String UPLOAD_CONTENT = "upC";
    private static final String GET_CONTENT = "gC";
    private static final String GET_MAPPING = "gm";

    private static final String PARAM_PROVIDED_MESSAGES_AMOUNT = "${providedMessagesNumber}";
    private static final String PARAM_EXPECTED_MESSAGES_AMOUNT = "${expectedMessagesAmount}";
    private static final String PARAM_MESSAGE_INDEX = "${messageIndex}";
    
    private static final String BUTTON_GET_CONTENT = "button_get_content";
    private static final String BUTTON_UPLOAD_CONTENT = "button_upload_content";
    private static final String BUTTON_GET_MAPPING = "button_get_mapping";
    
    private static final String MENU_CONTENT_ACTIONS_PAGE_0 = "menu_content_actions_page_0";

    private static final String SERVICE_MAPPING_ID_REQUEST = "service_mapping_id_request";

    private static final String ERROR_AMOUNT_OF_MESSAGES = "error_amount_of_messages";
    private static final String ERROR_PARSE_ID_FAILURE = "error_parse_id_failure";
    private static final String ERROR_TEXT_MESSAGE_EXPECTED = "error_text_message_expected";

    private final GetContentButtonHandler getContentHandler;
    private final UploadContentButtonHandler uploadContentHandler;

    private final MenuService menuService;

    private final ContentSessionService sessionService;

    private final SecurityService securityService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    public void configure() {
        final Menu menu = new Menu();
        final Page page = new Page();
        page.setMenu(menu);
        page.setPageIndex(0);
        page.setButtonsRowSize(2);
        page.setLocalizationFunction((u, p, b) -> localizationLoader.getLocalizationForUser(
                MENU_CONTENT_ACTIONS_PAGE_0, u));
        page.setButtonsFunction((u, p, b) -> List.of(new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_UPLOAD_CONTENT, u)
                    .getData(), UPLOAD_CONTENT, uploadContentHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_GET_CONTENT, u)
                    .getData(), GET_CONTENT, getContentHandler), new TerminalButton(
                localizationLoader.getLocalizationForUser(BUTTON_GET_MAPPING, u)
                    .getData(), GET_MAPPING, (b1, u1, pa) -> {
                    if (!securityService.grantAccess(b, u, AuthorityType.CONTENT_SETTINGS)) {
                        return;
                    }
                    sessionService.createSession(u1, b1, m -> {
                        if (m.size() != 1) {
                            final Map<String, Object> parameterMap = new HashMap<>();
                            parameterMap.put(PARAM_EXPECTED_MESSAGES_AMOUNT, 1);
                            parameterMap.put(PARAM_PROVIDED_MESSAGES_AMOUNT, m.size());
                            throw new InvalidDataSentException("One message was expected but "
                                + m.size() + " was/were sent", localizationLoader
                                .getLocalizationForUser(ERROR_AMOUNT_OF_MESSAGES,
                                u, parameterMap));
                        }
                        if (!m.get(0).hasText()) {
                            throw new InvalidDataSentException(
                                "The message is supposed to be the mapping id",
                                localizationLoader.getLocalizationForUser(
                                ERROR_TEXT_MESSAGE_EXPECTED, u, PARAM_MESSAGE_INDEX, 0));
                        }
                        final Long mappingId;
                        try {
                            mappingId = Long.parseLong(m.get(0).getText());
                        } catch (NumberFormatException e) {
                            throw new InvalidDataSentException("Unable to parse string "
                                + m.get(0).getText() + " to the mapping id", localizationLoader
                                .getLocalizationForUser(ERROR_PARSE_ID_FAILURE, u));
                        }
                        menuService.initiateMenu(MAPPING_MENU_NAME, u1, mappingId.toString(), b1);
                    }, true);
                    clientManager.getClient(b).sendMessage(u1, localizationLoader
                        .getLocalizationForUser(SERVICE_MAPPING_ID_REQUEST, u1));
                })));
        menu.setName(MENU_NAME);
        menu.setPages(List.of(page));
        menu.setInitialParameterPresent(false);
        menu.setOneTimeMenu(false);
        menuService.save(menu);
    }
}
