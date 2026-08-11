package com.unbidden.telegramcoursesbot.service.menu;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

public enum MenuKey {
    AVAILABLE_COURSES("m_aCrs"),
    ADMIN_ACTIONS("m_admAct"),
    BOT("m_bots"),
    CONTENT("m_cntAct"),
    COURSE_NEXT_STAGE("m_crsNxtStg"),
    COURSE_SETTINGS("m_crsOpt"),
    COURSES("m_crs"),
    EXTERNAL_INVOICE("m_extInv"),
    FILE("m_fls"),
    GENERAL_BAN("m_gnrlBn"),
    GENERAL_POST("m_gnrlPst"),
    GET_REVIEWS("m_gRv"),
    LANGUAGE("m_sl"),
    LEAVE_ADVANCED_REVIEW("m_laR"),
    LEAVE_BASIC_REVIEW("m_lbR"),
    MAPPING_SETTINGS("m_mpgOpt"),
    MULTIPAGE_LIST("m_mpl"),
    MY_COURSES("m_myCrs"),
    POST("m_pst"),
    REFRESH("m_rfsh"),
    REQUEST_FEEDBACK("m_rqF"),
    REVIEW_ACTIONS("m_rwA"),
    SEND_HOMEWORK("m_sHw"),
    STATISTICS("m_stat"),
    SUPPORT_REPLY("m_rpl"),
    SUPPORT_REPLY_TO_REPLY("m_rplToRpl"),
    SUPPORT_REQUEST("m_sr"),
    TEST("m_tst"),
    COMMIT_CONTENT("m_cmtCnt");

    private static final Map<String, MenuKey> REVERSED_KEY_MAP = new HashMap<>();

    static {
        for (MenuKey key : values()) {
            REVERSED_KEY_MAP.put(key.getName(), key);
        }
    }

    public static MenuKey getKeyByLocName(String name) {
        Assert.notNull(name, "name cannot be null");

        final MenuKey key = REVERSED_KEY_MAP.get(name);

        if (key == null) throw new RuntimeException("No menu key was found for menu name " + name + ".");

        return key;
    }
    
    private String name;

    MenuKey(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
