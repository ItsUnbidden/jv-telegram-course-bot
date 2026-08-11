package com.unbidden.telegramcoursesbot.service.menu;

import com.unbidden.telegramcoursesbot.exception.MenuExpiredException;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.menu.handler.ButtonHandler;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.function.TriFunction;

@Data
public class Menu {
    private static final String BACK_DATA = "back";
    private static final String LINK_DATA = "link";

    private final MenuKey key;

    private List<Page> pages;

    private boolean isInitialParameterPresent;

    private boolean isOneTimeMenu;

    private boolean isUpdateAfterTerminalButtonRequired;

    private boolean isAttachedToMessage;

    public Menu(MenuKey key) {
        this.key = key;
    }

    @Data
    public static class Page {
        private final Menu menu;

        private int pageIndex;

        private int previousPage;

        private int buttonsRowSize;

        private TriFunction<UserEntity, List<String>, Bot, Localization> localizationFunction;

        private TriFunction<UserEntity, List<String>, Bot, List<Button>> buttonsFunction;

        public Page(Menu menu) {
            this.menu = menu;
        }

        public Button getButtonByData(UserEntity user, Bot bot, String currentButtonData,
                String[] params) throws MenuExpiredException {
            List<Button> potentialButton = buttonsFunction.apply(user, Arrays.asList(params), bot)
                    .stream()
                    .filter(b -> b.getData().equals(currentButtonData))
                    .toList();
            if (potentialButton.isEmpty()) {
                throw new MenuExpiredException("There is no button with data "
                        + currentButtonData);
            }
            if (potentialButton.size() > 1) {
                throw new MenuExpiredException("There seem to be several buttons with data "
                        + currentButtonData);
            }
            return potentialButton.get(0);
        }

        @Data()
        @EqualsAndHashCode(callSuper = true)
        public static class TerminalButton extends Button {
            private ButtonHandler handler;

            public TerminalButton(String name, String data, ButtonHandler handler) {
                super(name, data, Type.TERMINAL);
                this.handler = handler;
            }

            public TerminalButton(String name, String id, String param, ButtonHandler handler) {
                super(name, id + Button.ID_PARAM_SEPARATOR + param, Type.TERMINAL);
                this.handler = handler;
            }
        }

        @Data()
        @EqualsAndHashCode(callSuper = true)
        public static class LinkButton extends Button {
            private String url;

            public LinkButton(String name, String url) {
                super(name, LINK_DATA, Type.LINK);
                this.url = url;
            }
        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class TransitoryButton extends Button {
            private int pagePointer;

            public TransitoryButton(String name, String data, int pagePointer) {
                super(name, data, Type.TRANSITORY);
                this.pagePointer = pagePointer;
            }

            public TransitoryButton(String name, String id, String param, int pagePointer) {
                super(name, id + Button.ID_PARAM_SEPARATOR + param, Type.TRANSITORY);
                this.pagePointer = pagePointer;
            }
        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class BackwardButton extends Button {
            public BackwardButton(String name) {
                super(name, BACK_DATA, Type.BACKWARD);
            }
        }

        @Data
        public abstract static class Button {
            public static final String ID_PARAM_SEPARATOR = "~";

            private String name;

            private String data;

            private Type type;

            public Button(String name, String data, Type type) {
                this.name = name;
                this.data = data;
                this.type = type;
            }

            public static enum Type {
                TERMINAL,
                LINK,
                TRANSITORY,
                BACKWARD
            }
        }
    }
}
