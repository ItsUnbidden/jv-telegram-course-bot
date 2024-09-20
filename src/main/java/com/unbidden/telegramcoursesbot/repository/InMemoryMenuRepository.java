package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryMenuRepository implements MenuRepository {
    private static final Map<String, Menu> menus = new HashMap<>();

    @NonNull
    @Override
    public Menu save(@NonNull Menu menu) {
        menus.put(menu.getName(), menu);
        return menu;
    }

    @NonNull
    @Override
    public Optional<Menu> find(@NonNull String menuName) {
        return Optional.ofNullable(menus.get(menuName));
    }
}
