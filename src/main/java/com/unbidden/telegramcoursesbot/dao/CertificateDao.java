package com.unbidden.telegramcoursesbot.dao;

import java.io.InputStream;

public interface CertificateDao extends FileDao {
    /**
     * Supposedly is not that important, but is required for the webhook to initialize.
     */
    public static final String PUBLIC_KEY_FILE_NAME = "YOURPUBLIC.pem";

    InputStream readPublicKey();
}
