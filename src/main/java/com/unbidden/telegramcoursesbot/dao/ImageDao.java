package com.unbidden.telegramcoursesbot.dao;

import org.springframework.lang.NonNull;

public interface ImageDao extends FileDao {
    @NonNull
    byte[] read(@NonNull String courseName);
}
