package com.unbidden.telegramcoursesbot.dao;

import java.nio.file.Path;
import org.springframework.lang.NonNull;

public interface LocalizationDao {
    @NonNull
    String getText(@NonNull Path path);

    boolean exists(@NonNull Path path);
}
