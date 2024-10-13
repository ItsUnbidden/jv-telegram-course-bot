package com.unbidden.telegramcoursesbot.dao;

import java.io.InputStream;
import org.springframework.lang.NonNull;

public interface LogDao extends FileDao {
    @NonNull
    InputStream readCurrentLogFile();
}
