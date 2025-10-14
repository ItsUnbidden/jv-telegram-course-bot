package com.unbidden.telegramcoursesbot.service.menu;

import com.unbidden.telegramcoursesbot.exception.MenuExpiredException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.menu.handler.ButtonHandler;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.function.TriFunction;

@Data
public class Menu {
    private static final String BACK_DATA = "back";

    private String name;

    private List<Page> pages;

    private boolean isInitialParameterPresent;

    private boolean isOneTimeMenu;

    private boolean isUpdateAfterTerminalButtonRequired;

    private boolean isAttachedToMessage;

    @Data
    public static class Page {
        private int pageIndex;

        private int previousPage;

        private Menu menu;

        private int buttonsRowSize;

        private TriFunction<UserEntity, List<String>, Bot, Localization> localizationFunction;

        private TriFunction<UserEntity, List<String>, Bot, List<Button>> buttonsFunction;

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
        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class TransitoryButton extends Button {
            private int pagePointer;

            public TransitoryButton(String name, String data, int pagePointer) {
                super(name, data, Type.TRANSITORY);
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
            private String name;

            private String data;

            private Type type;

            public Button(String name, String data, Type type) {
                this.name = name;
                this.data = data;
                this.type = type;
            }

            public enum Type {
                TERMINAL,
                TRANSITORY,
                BACKWARD
            }
        }
    }
}
