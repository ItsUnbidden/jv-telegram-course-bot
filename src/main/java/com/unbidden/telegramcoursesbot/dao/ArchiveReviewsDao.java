package com.unbidden.telegramcoursesbot.dao;

import java.io.InputStream;
import java.nio.file.Path;
import org.springframework.lang.NonNull;

public interface ArchiveReviewsDao extends FileDao {
    @NonNull
    Path createTempFile(@NonNull String name);

    @NonNull
    Path write(@NonNull Path path, @NonNull String content);

    @NonNull
    InputStream read(@NonNull Path path);

    void delete(@NonNull Path path);
}
