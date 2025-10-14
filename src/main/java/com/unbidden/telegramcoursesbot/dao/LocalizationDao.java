package com.unbidden.telegramcoursesbot.dao;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import org.springframework.lang.NonNull;

public interface LocalizationDao extends FileDao {
    @NonNull
    Path createLocalizationsDir();

    @NonNull
    Path createLanguageSubDir(@NonNull String languageCode);

    @NonNull
    Path addOrUpdateLocalizationsFile(@NonNull InputStream is, @NonNull String fileName,
            @NonNull String languageCode);

    @NonNull
    String getText(@NonNull Path path);

    boolean exists(@NonNull Path path);

    @NonNull
    List<Path> listLocalizationDirs();

    @NonNull
    List<Path> list(@NonNull Path path);
}
