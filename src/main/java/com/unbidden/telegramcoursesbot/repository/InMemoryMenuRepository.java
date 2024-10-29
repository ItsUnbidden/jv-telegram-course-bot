package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryMenuRepository implements MenuRepository {
    private static final ConcurrentMap<String, Menu> menus = new ConcurrentHashMap<>();

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
