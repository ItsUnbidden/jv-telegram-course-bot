package com.unbidden.telegramcoursesbot.dao;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public interface LocalizationDao extends FileDao {
    Path createLocalizationsDir();

    Path createLanguageSubDir(String languageCode);

    Path addOrUpdateLocalizationsFile(InputStream is, String fileName, String languageCode);

    String getText(Path path);

    boolean exists(Path path);

    List<Path> listLocalizationDirs();

    List<Path> list(Path path);
}
