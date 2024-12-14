package com.unbidden.telegramcoursesbot.model;

public enum AuthorityType {
    /**
     * Toggle maintenance (0)
     */
    MAINTENANCE,
    /**
     * Change course settings (1)
     */
    COURSE_SETTINGS,
    /**
     * Give or take courses (2)
     */
    GIVE_COURSE,
    /**
     * See and comment reviews (3)
     */
    SEE_REVIEWS,
    /**
     * Give homework feedback (4)
     */
    GIVE_HOMEWORK_FEEDBACK,
    /**
     * Handle support queries (5)
     */
    ANSWER_SUPPORT,
    /**
     * Initiate a course (6)
     */
    LAUNCH_COURSE,
    /**
     * Leave a review (7)
     */
    LEAVE_REVIEW,
    /**
     * Initiate a support query (8)
     */
    ASK_SUPPORT,
    /**
     * Buy things (9)
     */
    BUY,
    /**
     * Manage user roles (10 )
     */
    ROLE_SETTINGS,
    /**
     * Create posts (11)
     */
    POST,
    /**
     * Initiate refunds (12)
     */
    REFUND,
    /**
     * Manage content and localizations (13)
     */
    CONTENT_SETTINGS,
    /**
     * See info such as creator page or start (14)
     */
    INFO,
    /**
     * Bot-specific bans (15)
     */
    BOT_USER_BANS,
    /**
     * All-in bans (16)
     */
    GENERAL_BANS,
    /**
     * Two-way reply (17)
     */
    REPLY_SUPPORT,
    /**
     * List, create and disable bots (18)
     */
    BOTS_SETTINGS
}
