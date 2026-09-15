package com.unbidden.telegramcoursesbot.menu;

import com.unbidden.telegramcoursesbot.dto.internal.MenuParamsDto;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.menu.handler.AbstractButtonHandler;
import com.unbidden.telegramcoursesbot.model.BackwardMenuSnapshotButton;
import com.unbidden.telegramcoursesbot.model.GeneralMenuSnapshot;
import com.unbidden.telegramcoursesbot.model.MenuSnapshotButton;
import com.unbidden.telegramcoursesbot.model.TerminalMenuSnapshotButton;
import com.unbidden.telegramcoursesbot.model.TransitoryMenuSnapshotButton;
import com.unbidden.telegramcoursesbot.model.UrlMenuSnapshotButton;
import com.unbidden.telegramcoursesbot.util.MenuUtil;

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
            private final List<String> paramNames;

            private final List<String> paramValues;

            private final AbstractButtonHandler handler;

            public TerminalButton(String name, AbstractButtonHandler handler) {
                super(name);
                this.handler = handler;
                this.paramNames = List.of();
                this.paramValues = List.of();
            }

            public TerminalButton(String name, String param, AbstractButtonHandler handler) {
                super(name);
                this.handler = handler;
                this.paramNames = List.of();
                this.paramValues = List.of(param);
            }

            public TerminalButton(String name, String paramName, String paramValue, AbstractButtonHandler handler) {
                super(name);
                this.handler = handler;
                this.paramNames = List.of(paramName);
                this.paramValues = List.of(paramValue);
            }

            public TerminalButton(String name, List<String> paramNames, List<String> paramValues, AbstractButtonHandler handler) {
                super(name);
                this.handler = handler;
                this.paramNames = paramNames;
                this.paramValues = paramValues;
            }

            @Override
            public MenuSnapshotButton toMenuSnapshotButton(GeneralMenuSnapshot snapshot, MenuUtil util) {
                final var button = new TerminalMenuSnapshotButton();
                
                button.setSnapshot(snapshot);
                button.setParamNames(util.listToString(paramNames));
                button.setParamValues(util.listToString(paramValues));
                button.setHandlerBeanName(handler.getBeanName());

                return button;
            }
        }

        @Data()
        @EqualsAndHashCode(callSuper = true)
        public static class LinkButton extends Button {
            private final String url;

            public LinkButton(String name, String url) {
                super(name);
                this.url = url;
            }

            @Override
            public MenuSnapshotButton toMenuSnapshotButton(GeneralMenuSnapshot snapshot, MenuUtil util) {
                final var button = new UrlMenuSnapshotButton();
                
                button.setSnapshot(snapshot);

                return button;
            }
        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class TransitoryButton extends Button {
            private final List<String> paramNames;

            private final List<String> paramValues;

            private final int pagePointer;

            public TransitoryButton(String name, int pagePointer) {
                super(name);
                this.pagePointer = pagePointer;
                this.paramNames = List.of();
                this.paramValues = List.of();
            }

            public TransitoryButton(String name, String paramName, String paramValue, int pagePointer) {
                super(name);
                this.pagePointer = pagePointer;
                this.paramNames = List.of(paramName);
                this.paramValues = List.of(paramValue);
            }

            public TransitoryButton(String name, List<String> paramNames, List<String> paramValues, int pagePointer) {
                super(name);
                this.pagePointer = pagePointer;
                this.paramNames = paramNames;
                this.paramValues = paramValues;
            }

            @Override
            public MenuSnapshotButton toMenuSnapshotButton(GeneralMenuSnapshot snapshot, MenuUtil util) {
                final var button = new TransitoryMenuSnapshotButton();
                
                button.setSnapshot(snapshot);
                button.setParamNames(util.listToString(paramNames));
                button.setParamValues(util.listToString(paramValues));
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
            public MenuSnapshotButton toMenuSnapshotButton(GeneralMenuSnapshot snapshot, MenuUtil util) {
                final var button = new BackwardMenuSnapshotButton();
                
                button.setSnapshot(snapshot);

                return button;
            }
        }

        @Data
        public abstract static class Button {
            private final String name;

            public Button(String name) {
                this.name = name;
            }

            public abstract MenuSnapshotButton toMenuSnapshotButton(GeneralMenuSnapshot snapshot, MenuUtil util);
        }
    }
}
