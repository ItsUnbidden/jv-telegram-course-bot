package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.service.menu.Menu;
import com.unbidden.telegramcoursesbot.service.menu.MenuKey;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryMenuRepository implements MenuRepository {
    private static final ConcurrentMap<MenuKey, Menu> menus = new ConcurrentHashMap<>();

    @NonNull
    @Override
    public Menu save(@NonNull Menu menu) {
        menus.put(menu.getKey(), menu);
        return menu;
    }

    @NonNull
    @Override
    public Optional<Menu> find(MenuKey key) {
        return Optional.ofNullable(menus.get(key));
    }
}
