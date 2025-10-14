package com.unbidden.telegramcoursesbot.dao;

import java.io.InputStream;
import java.nio.file.Path;
import org.springframework.lang.NonNull;

public interface ImageDao extends FileDao {
    @NonNull
    Path createDir();

    @NonNull
    Path addOrUpdateImage(@NonNull InputStream is, @NonNull String courseName);

    @NonNull
    byte[] read(@NonNull String courseName);

    boolean exists(@NonNull String courseName);

    void delete(@NonNull String courseName);
}
