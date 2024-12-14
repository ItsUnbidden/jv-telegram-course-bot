package com.unbidden.telegramcoursesbot.model;

public enum RoleType {
    /**
     * 19 (all) authorities. Automatically added to every new bot created for a specified 
     * user (application.properties -> telegram.bot.authorization.director.id).
     */
    DIRECTOR,
    /**
     * 1 COURSE_SETTINGS, 2 GIVE_COURSE, 3 SEE_REVIEWS, 4 GIVE_HOMEWORK_FEEDBACK, 
     * 5 ANSWER_SUPPORT, 10 ROLE_SETTINGS, 11 POST, 13 CONTENT_SETTINGS, 14 INFO, 
     * 15 BOT_USER_BANS, REPLY_SUPPORT 17. Automatically added to every new bot 
     * created for user who created that bot.
     */
    CREATOR,
    /**
     * 5 ANSWER_SUPPORT, 15 BOT_USER_BANS, REPLY_SUPPORT 17, 14 INFO. Can provide support to
     * users. Authorities may be expanded in the future.
     */
    SUPPORT,
    /**
     * 3 SEE_REVIEWS, 4 GIVE_HOMEWORK_FEEDBACK, 11 POST, 14 INFO, 15 BOT_USER_BANS. Can give 
     * feedback for homeworks and post messages.
     */
    MENTOR,
    /**
     * 6 LAUNCH_COURSE, 7 LEAVE_REVIEW, 8 ASK_SUPPORT, 9 BUY, 12 REFUND, 14 INFO,
     * REPLY_SUPPORT 17. Automatically given to all users who are not of any other role.
     */
    USER,
    /**
     * Special role to indicate that user has no authorities (no actions are
     * permitted in a specific bot).
     */
    BANNED
}
