package com.unbidden.telegramcoursesbot.service.button.menu;

import com.unbidden.telegramcoursesbot.service.button.handler.ButtonHandler;
import java.util.List;
import java.util.function.Function;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.telegram.telegrambots.meta.api.objects.User;

@Data
public class Menu {
    private String name;

    private List<Page> pages;

    @Data
    public static class Page {
        private int pageIndex;

        private Type type;

        private Menu menu;

        private Function<User, String> textFunction;

        private Function<User, List<Button>> buttonsFunction;

        public Button getButtonByData(User user, String data) {
            List<Button> potentialButton = buttonsFunction.apply(user).stream()
                    .filter(b -> b.getData().equals(data))
                    .toList();
            if (potentialButton.isEmpty()) {
                throw new IllegalArgumentException("There is no button with data " + data);
            }
            if (potentialButton.size() > 1) {
                throw new IllegalArgumentException("There seem to be several buttons with data "
                        + data);
            }
            return potentialButton.get(0);
        }

        @Data()
        @EqualsAndHashCode(callSuper = true)
        public static class TerminalButton extends Button {
            private ButtonHandler handler;

            public TerminalButton(String name, String data, ButtonHandler handler) {
                super(name, data);
                this.handler = handler;
            }
        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class TransitoryButton extends Button {
            public TransitoryButton(String name, String data) {
                super(name, data);
            }
        }

        @Data
        public abstract static class Button {
            private String name;

            private String data;

            public Button(String name, String data) {
                this.name = name;
                this.data = data;
            }
        }

        public enum Type {
            TERMINAL,
            TRANSITORY
        }
    }
}
