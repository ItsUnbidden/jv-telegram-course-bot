package com.unbidden.telegramcoursesbot.dao;

import java.nio.file.Path;
import org.springframework.lang.NonNull;

public interface ImageDao extends FileDao {
    byte[] read(@NonNull Path path);
}
