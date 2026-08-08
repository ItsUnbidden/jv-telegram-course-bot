package com.unbidden.telegramcoursesbot.dao;

import java.io.InputStream;
import java.nio.file.Path;

public interface ImageDao extends FileDao {
    Path createDir();

    Path addOrUpdateImage(InputStream is, Long courseId);

    byte[] read(Long courseId);

    boolean exists(Long courseId);

    void delete(Long courseId);
}
