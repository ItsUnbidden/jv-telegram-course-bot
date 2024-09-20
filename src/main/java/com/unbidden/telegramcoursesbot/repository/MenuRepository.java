package com.unbidden.telegramcoursesbot.repository;

import com.unbidden.telegramcoursesbot.service.button.menu.Menu;
import java.util.Optional;
import org.springframework.lang.NonNull;

public interface MenuRepository {
    @NonNull
    Menu save(@NonNull Menu menu);

    @NonNull
    Optional<Menu> find(@NonNull String menuName);
}
