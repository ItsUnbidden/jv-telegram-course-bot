package com.unbidden.telegramcoursesbot.dao;

import java.nio.file.Path;
import org.springframework.lang.NonNull;

public interface ImageDao extends FileDao {
    @NonNull
    Path createDir();

    @NonNull
    byte[] read(@NonNull String courseName);

    boolean isPresent(@NonNull String courseName);

    boolean exists(@NonNull Path path);
}
