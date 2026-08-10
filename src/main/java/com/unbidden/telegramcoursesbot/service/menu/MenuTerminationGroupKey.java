package com.unbidden.telegramcoursesbot.service.menu;

public enum MenuTerminationGroupKey {
    /**
     * Requires session ID.
     */
    COMMIT_CONTENT("session_%s_terminator"),
    /**
     * Requires homework progress ID.
     */
    REQUEST_FEEDBACK("homework_progress_%s_feedback_menus"),
    /**
     * Requires homework progress ID.
     */
    SEND_HOMEWORK("homework_progress_%s_send_homework_menus"),
    /**
     * Requires review ID.
     */
    REVIEW_ACTIONS("review_%s_actions"),
    /**
     * Requires course ID.
     */
    LEAVE_BASIC_REVIEW("course_%s_send_basic_review"),
    /**
     * Requires review ID.
     */
    LEAVE_ADVANCED_REVIEW("review_%s_send_advanced_review"),
    /**
     * Requires support request ID.
     */
    SUPPORT_REPLY("support_request_%s_reply_menus"),
    /**
     * Requires course progress ID.
     */
    COURSE_NEXT_STAGE("course_progress_%s_next_stage");

    private String name;

    MenuTerminationGroupKey(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
