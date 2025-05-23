package com.unbidden.telegramcoursesbot.dao;

import java.nio.file.Path;
import java.util.List;
import org.springframework.lang.NonNull;

public interface LocalizationDao extends FileDao {
    @NonNull
    String getText(@NonNull Path path);

    boolean exists(@NonNull Path path);

    @NonNull
    List<Path> list(@NonNull Path path);
}
