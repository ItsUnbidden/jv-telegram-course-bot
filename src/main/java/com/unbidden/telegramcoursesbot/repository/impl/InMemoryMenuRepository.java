package com.unbidden.telegramcoursesbot.repository.impl;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.unbidden.telegramcoursesbot.menu.Menu;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.repository.MenuRepository;

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
