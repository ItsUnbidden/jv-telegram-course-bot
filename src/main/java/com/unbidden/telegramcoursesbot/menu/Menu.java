package com.unbidden.telegramcoursesbot.menu;

import com.unbidden.telegramcoursesbot.dto.internal.MenuParamsDto;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.menu.handler.AbstractButtonHandler;
import com.unbidden.telegramcoursesbot.model.BackwardMenuSnapshotButton;
import com.unbidden.telegramcoursesbot.model.MenuSnapshot;
import com.unbidden.telegramcoursesbot.model.MenuSnapshotButton;
import com.unbidden.telegramcoursesbot.model.TerminalMenuSnapshotButton;
import com.unbidden.telegramcoursesbot.model.TransitoryMenuSnapshotButton;
import com.unbidden.telegramcoursesbot.model.UrlMenuSnapshotButton;

import java.util.List;
import java.util.function.Function;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class Menu {
    private final MenuKey key;

    private List<Page> pages;

    private Page terminalPage;

    private boolean isOneTimeMenu;

    private boolean isResetAfterTerminal;

    public Menu(MenuKey key) {
        this.key = key;
    }

    @Data
    public static class Page {
        private final Menu menu;

        private int pageIndex;

        private int columns;

        /**
         * Currently doesn't do anything.
         */
        private int rows;

        private Function<MenuParamsDto, Localization> localizationFunction;

        private Function<MenuParamsDto, List<Button>> buttonsFunction;

        public Page(Menu menu) {
            this.menu = menu;
        }

        @Data()
        @EqualsAndHashCode(callSuper = true)
        public static class TerminalButton extends Button {
            private String paramName;

            private String paramValue;

            private AbstractButtonHandler handler;

            public TerminalButton(String name, AbstractButtonHandler handler) {
                super(name);
                this.handler = handler;
            }

            public TerminalButton(String name, String param, AbstractButtonHandler handler) {
                super(name);
                this.handler = handler;
                this.paramValue = param;
            }

            public TerminalButton(String name, String paramName, String paramValue, AbstractButtonHandler handler) {
                super(name);
                this.handler = handler;
                this.paramValue = paramValue;
                this.paramName = paramName;
            }

            @Override
            public MenuSnapshotButton toMenuSnapshotButton(MenuSnapshot snapshot) {
                final var button = new TerminalMenuSnapshotButton();
                
                button.setSnapshot(snapshot);
                button.setParamName(button.getParamName());
                button.setParamValue(button.getParamValue());
                button.setHandlerBeanName(handler.getBeanName());

                return button;
            }
        }

        @Data()
        @EqualsAndHashCode(callSuper = true)
        public static class LinkButton extends Button {
            private String url;

            public LinkButton(String name, String url) {
                super(name);
                this.url = url;
            }

            @Override
            public MenuSnapshotButton toMenuSnapshotButton(MenuSnapshot snapshot) {
                final var button = new UrlMenuSnapshotButton();
                
                button.setSnapshot(snapshot);

                return button;
            }
        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class TransitoryButton extends Button {
            private String paramName;

            private String paramValue;

            private int pagePointer;

            public TransitoryButton(String name, int pagePointer) {
                super(name);
                this.pagePointer = pagePointer;
            }

            public TransitoryButton(String name, String paramName, String paramValue, int pagePointer) {
                super(name);
                this.pagePointer = pagePointer;
                this.paramName = paramName;
                this.paramValue = paramValue;
            }

            @Override
            public MenuSnapshotButton toMenuSnapshotButton(MenuSnapshot snapshot) {
                final var button = new TransitoryMenuSnapshotButton();
                
                button.setSnapshot(snapshot);
                button.setParamName(paramName);
                button.setParamValue(paramValue);
                button.setPointer(pagePointer);

                return button;
            }
        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class BackwardButton extends Button {
            public BackwardButton(String name) {
                super(name);
            }

            @Override
            public MenuSnapshotButton toMenuSnapshotButton(MenuSnapshot snapshot) {
                final var button = new BackwardMenuSnapshotButton();
                
                button.setSnapshot(snapshot);

                return button;
            }
        }

        @Data
        public abstract static class Button {
            private String name;

            public Button(String name) {
                this.name = name;
            }

            public abstract MenuSnapshotButton toMenuSnapshotButton(MenuSnapshot snapshot);
        }
    }
}
