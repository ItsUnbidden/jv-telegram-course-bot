package com.unbidden.telegramcoursesbot.localization;

import java.time.LocalDateTime;
import java.util.List;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.CourseInvoice.PaymentType;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;

public final class Localizations {
    public static interface LocalizationKey {
        String getLocName();
    }

    public static enum Menu implements LocalizationKey {
        /**
         * This is a generic localization that is intended to be used with {@code localizeGeneric} method in {@link LocalizationLoader}.
         * It requires a <b>lowercase command name without the {@code /}</b> to be passed as an argument.
         */
        COMMAND_DESCRIPTION("menu_command_%s_description"),
        ADMIN_ACTIONS_PAGE_0("menu_admin_actions_page_0"),
        ADMIN_ACTIONS_PAGE_1("menu_admin_actions_page_1"),
        ADMIN_ACTIONS_PAGE_2("menu_admin_actions_page_2"),
        ADMIN_ACTIONS_PAGE_3("menu_admin_actions_page_3"),
        COURSES_PAGE_0("menu_courses_page_0"),
        COURSES_PAGE_1("menu_courses_page_1"),
        BOT_PAGE_0("menu_bot_page_0"),
        BOT_PAGE_1("menu_bot_page_1"),
        LANGUAGE_PAGE_0("menu_language_page_0"),
        MY_COURSES_PAGE_0("menu_my_courses_page_0"),
        MY_COURSES_PAGE_1("menu_my_courses_page_1"),
        MY_COURSES_PAGE_2("menu_my_courses_page_2"),
        MY_COURSES_PAGE_3("menu_my_courses_page_3"),
        MY_COURSES_PAGE_4("menu_my_courses_page_4"),
        MY_COURSES_PAGE_5("menu_my_courses_page_5"),
        MY_COURSES_PAGE_6("menu_my_courses_page_6"),
        MY_COURSES_PAGE_7("menu_my_courses_page_7"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>mappingId</li>
         *  <li>position</li>
         *  <li>content</li>
         * </ls>
         */
        MAPPING_SETTINGS_PAGE_0("menu_mapping_settings_page_0"),
        STATISTICS_PAGE_0("menu_statistics_page_0"),
        STATISTICS_PAGE_1("menu_statistics_page_1"),
        STATISTICS_PAGE_2("menu_statistics_page_2"),
        /**
         * Possible parameters:t
         * <ls>
         *  <li>usersOnStage</li>
         * </ls>
         */
        STATISTICS_PAGE_3("menu_statistics_page_3"),
        LEAVE_BASIC_REVIEW_PAGE_0("menu_leave_basic_review_page_0"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        LEAVE_BASIC_REVIEW_TERMINAL_PAGE("menu_leave_basic_review_terminal_page"),
        GENERAL_BAN_PAGE_0("menu_general_ban_page_0"),
        GENERAL_BAN_PAGE_1("menu_general_ban_page_1"),
        GENERAL_POST_PAGE_0("menu_general_post_page_0"),
        FILES_PAGE_0("menu_files_page_0"),
        GET_REVIEWS_PAGE_0("menu_get_reviews_page_0"),
        GET_REVIEWS_PAGE_1("menu_get_reviews_page_1"),
        POST_PAGE_0("menu_post_page_0"),
        POST_PAGE_1("menu_post_page_1"),
        REFRESH_PAGE_0("menu_refresh_page_0"),
        SUPPORT_REQUEST_PAGE_0("menu_support_request_page_0"),
        SUPPORT_REQUEST_PAGE_1("menu_support_request_page_1"),
        SUPPORT_REQUEST_TERMINAL_PAGE("menu_support_request_terminal_page"),
        RESOLVE_LAST_SUPPORT_REQUEST("button_resolve_last_support_request"),
        CONTENT_ACTIONS_PAGE_0("menu_content_actions_page_0"),
        COURSE_SETTINGS_PAGE_0("menu_course_settings_page_0"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>id</li>
         *  <li>localizedTitle</li>
         *  <li>titleId</li>
         *  <li>descriptionId</li>
         *  <li>endMappingId</li>
         *  <li>paymentType</li>
         *  <li>numberOfLessons</li>
         *  <li>price</li>
         *  <li>refundStage</li>
         *  <li>externalStoreUrl</li>
         *  <li>externalInvoiceMappingId</li>
         *  <li>isOnMaintenance</li>
         *  <li>isHomeworkIncluded</li>
         *  <li>isFeedbackIncluded</li>
         * </ls>
         */
        COURSE_SETTINGS_PAGE_1("menu_course_settings_page_1"),
        COURSE_SETTINGS_PAGE_2("menu_course_settings_page_2"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>lessonId</li>
         *  <li>index</li>
         *  <li>homeworkId</li>
         *  <li>delay</li>
         *  <li>nextLessonTitleMappingId</li>
         *  <li>mappingIds</li>
         * </ls>
         */
        COURSE_SETTINGS_PAGE_3("menu_course_settings_page_3"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>homeworkId</li>
         *  <li>lessonId</li>
         *  <li>delay</li>
         *  <li>homeworkMappingId</li>
         *  <li>homeworkMediaTypes</li>
         *  <li>homeworkFeedback</li>
         *  <li>homeworkRepeatedCompletion</li>
         * </ls>
         */
        COURSE_SETTINGS_PAGE_4("menu_course_settings_page_4"),
        COURSE_SETTINGS_PAGE_5("menu_course_settings_page_5"),
        COMMIT_CONTENT_PAGE_0("menu_commit_content_page_0"),
        COMMIT_CONTENT_TERMINAL_PAGE("menu_commit_content_terminal_page"),
        COMMIT_CONTENT_RESEND_TERMINAL_PAGE("menu_commit_content_resend_terminal_page"),
        COMMIT_CONTENT_CANCEL_TERMINAL_PAGE("menu_commit_content_cancel_terminal_page"),
        COMMIT_CONTENT_EXPIRED_TERMINAL_PAGE("menu_commit_content_expired_terminal_page");

        private String locName;

        Menu() {
            this.locName = "menu_" + this.name().toLowerCase();
        }

        Menu(String locName) {
            this.locName = locName;
        }

        @Override
        public String getLocName() {
            return locName;
        }

        public static record CourseSettingsPage1Params(long id, String localizedTitle, long titleId, String descriptionId,
                String endMappingId, PaymentType paymentType, int numberOfLessons, String price,
                String refundStage, String externalStoreUrl, String externalInvoiceMappingId, String isOnMaintenance,
                String isHomeworkIncluded, String isFeedbackIncluded) {}
        public static record CourseSettingsPage3Params(long lessonId, int index, String homeworkId, String delay,
                String nextLessonTitleMappingId, List<Long> mappingIds) {}
        public static record CourseSettingsPage4Params(long homeworkId, long lessonId, String delay,
                long homeworkMappingId, String homeworkMediaTypes,
                String homeworkFeedback, String homeworkRepeatedCompletion) {}
        public static record LeaveBasicReviewTerminalPageParams(String courseName) {}
        public static record MappingSettingsPage0Params(long mappingId, int position, String content) {}
        public static record StatisticsPage3Params(String usersOnStage) {}
    }

    public static enum Button implements LocalizationKey {
        BAN_OPTIONS("button_ban_options"),
        TOGGLE_RECEIVE_HOMEWORK("button_toggle_receive_homework"),
        LIST_ADMINS("button_list_admins"),
        ADD_OR_REMOVE_ADMIN("button_set_role"),
        BACK("button_back"),
        LIFT_BAN("button_lift_ban"),
        GIVE_BAN("button_give_ban"),
        CHOOSE_USER("button_choose_user"),
        LIST_BOTS("button_list_bots"),
        DISABLE_BOT("button_disable_bot"),
        CREATE_BOT("button_create_bot"),
        RESEND_CONTENT("button_resend_content"),
        CONFIRM_SEND_CONTENT("button_confirm_send_content"),
        CANCEL_SESSION("button_cancel_session"),
        GET_CONTENT("button_get_content"),
        UPLOAD_CONTENT("button_upload_content"),
        GET_MAPPING("button_get_mapping"),
        NEXT_LESSON_DEFAULT("button_next_lesson_default"),
        COURSE_HOMEWORK_SETTING("button_course_homework_setting"),
        COURSE_FEEDBACK_SETTING("button_course_feedback_setting"),
        GIVE_OR_TAKE_COURSE("button_give_or_take_course"),
        COURSE_PRICE_CHANGE("button_course_price_change"),
        CREATE_NEW_COURSE("button_create_new_course"),
        HOMEWORK_SETTINGS("button_homework_settings"),
        UPDATE_HOMEWORK_CONTENT("button_update_homework_content"),
        REMOVE_CONTENT_FROM_LESSON("button_remove_content_from_lesson"),
        ADD_CONTENT_TO_LESSON("button_add_content_to_lesson"),
        COURSE_LESSONS("button_course_lessons"),
        CREATE_HOMEWORK("button_create_homework"),
        HOMEWORK_REPEATED_COMPLETION("button_homework_repeated_completion"),
        HOMEWORK_FEEDBACK("button_homework_feedback"),
        UPDATE_MEDIA_TYPES("button_update_media_types"),
        REMOVE_COURSE("button_remove_course"),
        CHANGE_MAPPING_ORDER("button_change_mapping_order"),
        UPDATE_REFUND_STAGE("button_update_refund_stage"),
        CREATE_LESSON("button_create_lesson"),
        REMOVE_LESSON("button_remove_lesson"),
        SET_HOMEWORK_DELAY("button_set_homework_delay"),
        SET_LESSON_DELAY("button_set_lesson_delay"),
        TOGGLE_COURSE_MAINTENANCE("button_toggle_course_maintenance"),
        MY_COURSES("button_my_courses"),
        AVAILABLE_COURSES("button_available_courses"),
        SELECT_COURSE_STAGE("button_select_course_stage"),
        SEND_ADVANCED_REVIEW("button_send_advanced_review"),
        UPDATE_ADVANCED_REVIEW("button_update_advanced_review"),
        UPDATE_PLATFORM_GRADE("button_update_platform_grade"),
        UPDATE_COURSE_GRADE("button_update_course_grade"),
        LEAVE_REVIEW("button_leave_review"),
        UPDATE_REVIEW_OPTIONS("button_update_review_options"),
        BEGIN_COURSE("button_begin_course"),
        REFUND("button_refund"),
        EXTERNAL_INVOICE_MORE_INFO("button_external_invoice_more_info"),
        DELETE_INVOICE_IMAGE("button_delete_invoice_image"),
        UPLOAD_IMAGE_FILE("button_upload_image_file"),
        UPLOAD_LOCALIZATION_FILE("button_upload_localization_file"),
        POST_CUSTOM_ROLE_SET("button_post_custom_role_set"),
        GET_ARCHIVE_REVIEWS("button_get_archive_reviews"),
        GET_NEW_REVIEWS("button_get_new_reviews"),
        ALL_COURSES_REVIEWS("button_all_courses_reviews"),
        DEFAULT_LANGUAGE_CODE("button_default_language_code"),
        REMOVE_MAPPING_LOCALIZATION("button_remove_mapping_localization"),
        ADD_MAPPING_LOCALIZATION("button_add_mapping_localization"),
        SEND_PRIVATE_MESSAGE("button_send_private_message"),
        POST_OPTIONS("button_post_options"),
        MENUS_REFRESH("button_menus_refresh"),
        LOCALIZATIONS_REFRESH("button_localizations_refresh"),
        ACCEPT_HOMEWORK_WITH_COMMENT("button_accept_homework_with_comment"),
        ACCEPT_HOMEWORK("button_accept_homework"),
        GENERAL_ACCEPT_HOMEWORK("button_general_accept_homework"),
        DECLINE_HOMEWORK("button_decline_homework"),
        LEAVE_REVIEW_COMMENT("button_leave_review_comment"),
        GET_REVIEW_COMMENT("button_get_review_comment"),
        MARK_REVIEW_AS_READ("button_mark_review_as_read"),
        UPDATE_COMMENT("button_update_comment"),
        SEND_HOMEWORK("button_send_homework"),
        COURSE_STATISTICS("button_course_statistics"),
        BOT_USERS_STATISTICS("button_bot_users_statistics"),
        COURSE_USERS_STATISTICS("button_course_users_statistics"),
        GENERAL_BOT_STATISTICS("button_general_bot_statistics"),
        COURSE_USERS_BY_STAGE("button_course_users_by_stage"),
        COURSE_COMPLETED_USERS("button_course_completed_users"),
        COURSE_ALL_USERS("button_course_all_users"),
        REPLY_TO_SUPPORT_REQUEST("button_reply_to_support_request"),
        REPLY_TO_SUPPORT_REPLY("button_reply_to_support_reply"),
        RESOLVE_SUPPORT_REQUEST("button_resolve_support_request"),
        RESOLVE_LAST_SUPPORT_REQUEST("button_resolve_last_support_request"),
        BAN_CHOOSE_USER("button_ban_choose_user"),
        TAKE_COURSE("button_take_course"),
        GIVE_COURSE("button_give_course"),
        CREATE_COURSE_EXTERNAL_PAYMENT("button_create_course_external_payment"),
        CREATE_COURSE_TELEGRAM_PAYMENT("button_create_course_telegram_payment"),
        SET_ROLE_CHOOSE_USER("button_set_role_choose_user"),
        /**
         * A test localization. Is not used in any production systems.
         */
        TEST_MENU("button_test_menu"),
        BY_ID("button_by_id");

        private String locName;

        Button() {
            this.locName = "button_" + this.name().toLowerCase();
        }

        Button(String locName) {
            this.locName = locName;
        }

        public String getLocName() {
            return locName;
        }
    }

    public static enum Service implements LocalizationKey {
        /**
         * This is a generic localization that is intended to be used with {@code localizeGeneric} method in {@link LocalizationLoader}.
         * It requires a <b>lowercase role type</b> to be passed as an argument.
         */
        ROLE_TITLE("service_role_%s_title"),
        /**
         * This is a generic localization that is intended to be used with {@code localizeGeneric} method in {@link LocalizationLoader}.
         * It requires a <b>lowercase language code</b> to be passed as an argument.
         */
        LANGUAGE_CODE("service_language_code_%s"),
        NOT_AVAILABLE("service_not_available"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>currentPage</li>
         *  <li>numberOfPages</li>
         *  <li>numberOfElements</li>
         *  <li>data</li>
         *  <li>courseName</li>
         * </ls>
         */
        COURSE_COMPLETED_USERS("service_course_completed_users"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>title</li>
         *  <li>whoBanned</li>
         * </ls>
         */
        BANNED("service_banned"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>title</li>
         *  <li>hoursUntilLift</li>
         *  <li>whoBanned</li>
         * </ls>
         */
        TEMPORARY_BANNED("service_temporary_banned"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>title</li>
         *  <li>newRoleType</li>
         *  <li>whoChanged</li>
         * </ls>
         */
        ROLE_CHANGED("service_role_changed"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>title</li>
         *  <li>whoLifted</li>
         * </ls>
         */
        BAN_LIFTED("service_ban_lifted"),
        BAN_LIFTED_AUTO("service_ban_lifted_auto"),
        GENERAL_BAN_LIFTED_AUTO("service_general_ban_lifted_auto"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>title</li>
         *  <li>whoLifted</li>
         * </ls>
         */
        GENERAL_BAN_LIFTED("service_general_ban_lifted"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>title</li>
         *  <li>hoursUntilLift</li>
         *  <li>whoBanned</li>
         * </ls>
         */
        TEMPORARY_GENERAL_BAN("service_temporary_general_ban"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>title</li>
         *  <li>whoBanned</li>
         * </ls>
         */
        GENERAL_BAN("service_general_ban"),
        LESS_THEN_AN_HOUR("service_less_then_an_hour"),
        AN_HOUR("service_an_hour"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>hours</li>
         * </ls>
         */
        HOURS("service_hours"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>basicTimestamp</li>
         *  <li>lastUpdateTimestamp</li>
         *  <li>courseName</li>
         *  <li>courseGrade</li>
         *  <li>usersWhoRead</li>
         *  <li>userWhoCommented</li>
         *  <li>commentedAt</li>
         *  <li>contentId</li>
         *  <li>advancedTimestamp</li>
         * </ls>
         */
        REVIEW_INFO_CONTENT_COMMENT("service_review_info_content_comment"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>basicTimestamp</li>
         *  <li>lastUpdateTimestamp</li>
         *  <li>courseName</li>
         *  <li>courseGrade</li>
         *  <li>usersWhoRead</li>
         *  <li>userWhoCommented</li>
         *  <li>commentedAt</li>
         * </ls>
         */
        REVIEW_INFO_CONTENT("service_review_info_content_comment"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>basicTimestamp</li>
         *  <li>lastUpdateTimestamp</li>
         *  <li>courseName</li>
         *  <li>courseGrade</li>
         *  <li>usersWhoRead</li>
         *  <li>contentId</li>
         *  <li>advancedTimestamp</li>
         * </ls>
         */
        REVIEW_INFO_COMMENT("service_review_info_content_comment"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>basicTimestamp</li>
         *  <li>lastUpdateTimestamp</li>
         *  <li>courseName</li>
         *  <li>courseGrade</li>
         *  <li>usersWhoRead</li>
         * </ls>
         */
        REVIEW_INFO("service_review_info_content_comment"),
        NO_NEW_REVIEWS_FOR_USER("service_no_new_reviews_for_user"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        REVIEW_CONTENT_UPDATED("service_review_content_updated"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        REVIEW_PLATFORM_GRADE_UPDATED("service_review_platform_grade_updated"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        REVIEW_COURSE_GRADE_UPDATED("service_review_course_grade_updated"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>commenterFullName</li>
         *  <li>title</li>
         * </ls>
         */
        COMMENT_SUBMITTED_NOTIFICATION("service_comment_submitted_notification"),
        COMMENT_SUBMITTED("service_comment_submitted"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        ADVANCED_REVIEW_SUBMITTED("service_advanced_review_submitted"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        BASIC_REVIEW_SUBMITTED("service_basic_review_submitted"),
        ADVANCED_REVIEW_TERMINAL("service_advanced_review_terminal"),
        BASIC_REVIEW_TERMINAL("service_basic_review_terminal"),
        REVIEW_MEDIA_GROUP_BYPASS("service_review_media_group_bypass"),
        SEND_HOMEWORK_MEDIA_GROUP_BYPASS("service_send_homework_media_group_bypass"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>targetId</li>
         *  <li>targetFullName</li>
         *  <li>targetLanguage</li>
         *  <li>courseName</li>
         *  <li>lessonIndex</li>
         * </ls>
         */
        HOMEWORK_FEEDBACK_REQUEST_NOTIFICATION("service_homework_feedback_request_notification"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>lessonIndex</li>
         *  <li>whoDeclined</li>
         *  <li>title</li>
         * </ls>
         */
        HOMEWORK_DECLINED_NOTIFICATION_PLUS_COMMENT("service_homework_declined_notification_plus_comment"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>lessonIndex</li>
         *  <li>whoApproved</li>
         *  <li>title</li>
         * </ls>
         */
        HOMEWORK_APPROVED_NOTIFICATION("service_homework_approved_notification"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>lessonIndex</li>
         *  <li>whoApproved</li>
         *  <li>title</li>
         * </ls>
         */
        HOMEWORK_APPROVED_NOTIFICATION_PLUS_COMMENT("service_homework_approved_notification_plus_comment"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>targetId</li>
         *  <li>targetFullName</li>
         *  <li>targetLanguage</li>
         * </ls>
         */
        HOMEWORK_SUBMITTED_NOTIFICATION("service_homework_submitted_notification"),
        HOMEWORK_ACCEPTED_AUTO("service_homework_accepted_auto"),
        FEEDBACK_FOR_HOMEWORK_WAITING("service_feedback_for_homework_waiting"),
        FEEDBACK_MEDIA_GROUP_BYPASS("service_feedback_media_group_bypass"),
        SUPPORT_REQUEST_MEDIA_GROUP_BYPASS("service_support_request_media_group_bypass"),
        SUPPORT_REPLY_MEDIA_GROUP_BYPASS("service_support_reply_media_group_bypass"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>title</li>
         * </ls>
         */
        SUPPORT_REPLY_INFO("service_support_reply_info"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>timestamp</li>
         *  <li>tag</li>
         * </ls>
         */
        SUPPORT_INFO("service_support_info"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>title</li>
         * </ls>
         */
        SUPPORT_REQUEST_RESOLVED("service_support_request_resolved"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        REFUND_SUCCESS("service_refund_success"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>courseName</li>
         * </ls>
         */
        USER_REFUNDED_COURSE("service_user_refunded_course"),
        COURSE_EXTERNAL_INVOICE_MEDIA_GROUP_BYPASS("service_course_external_invoice_media_group_bypass"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        SUCCESSFUL_PAYMENT("service_successful_payment"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>courseName</li>
         * </ls>
         */
        USER_BOUGHT_COURSE("service_user_bought_course"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseId</li>
         *  <li>userId</li>
         * </ls>
         */
        AUTOMATIC_REFUND_NOTIFICATION("service_automatic_refund_notification"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        AUTOMATIC_REFUND("service_automatic_refund"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>targetFullName</li>
         *  <li>targetTitle</li>
         * </ls>
         */
        COURSE_GIFTED_SUCCESSFULLY("service_course_gifted_successfully"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>senderTitle</li>
         *  <li>senderFullName</li>
         * </ls>
         */
        COURSE_GIFTED_NOTIFICATION("service_course_gifted_notification"),
        RESEND_CONTENT("service_resend_content"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>numberOfCourses</li>
         *  <li>coursesBought</li>
         *  <li>coursesRefunded</li>
         *  <li>coursesCurrentlyOwned</li>
         *  <li>totalStarsIncome</li>
         *  <li>coursesGifted</li>
         *  <li>numberOfUsers</li>
         *  <li>numberOfBannedUsers</li>
         * </ls>
         */
        BOT_STATISTICS_REPORT("service_bot_statistics_report"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>timesBought</li>
         *  <li>timesRefunded</li>
         *  <li>totalStarsIncome</li>
         *  <li>numberOfOwners</li>
         *  <li>numberOfUsersWhoCompleted</li>
         *  <li>timesGifted</li>
         * </ls>
         */
        COURSE_STATISTICS_REPORT("service_course_statistics_report"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>currentPage</li>
         *  <li>numberOfPages</li>
         *  <li>numberOfElements</li>
         *  <li>data</li>
         *  <li>courseName</li>
         *  <li>stage</li>
         * </ls>
         */
        COURSE_STAGE_USERS("service_course_stage_users"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>currentPage</li>
         *  <li>numberOfPages</li>
         *  <li>numberOfElements</li>
         *  <li>data</li>
         *  <li>courseName</li>
         * </ls>
         */
        COURSE_USERS("service_course_users"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>currentPage</li>
         *  <li>numberOfPages</li>
         *  <li>numberOfElements</li>
         *  <li>data</li>
         * </ls>
         */
        BOT_USERS("service_bot_users"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>successes</li>
         *  <li>failures</li>
         * </ls>
         */
        POST_COMPLETED("service_post_completed"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>senderFullName</li>
         *  <li>title</li>
         * </ls>
         */
        PRIVATE_MESSAGE_INFO("service_private_message_info"),
        NO_CREATOR_INFO("service_no_creator_info"),
        NO_TERMS("service_no_terms"),
        NO_START("service_no_start"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>status</li>
         * </ls>
         */
        ON_MAINTENANCE_STATUS_CHANGE("service_on_maintenance_status_change"),
        STATUS_DISABLED("service_status_disabled"),
        STATUS_ENABLED("service_status_enabled"),
        MAPPING_ID_REQUEST("service_mapping_id_request"),
        APPROVE_HOMEWORK_COMMENT_REQUEST("service_approve_homework_comment_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>lessonId</li>
         * </ls>
         */
        ADD_LESSON_CONTENT_REQUEST("service_add_lesson_content_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>mappingId</li>
         *  <li>lessonId</li>
         * </ls>
         */
        LESSON_CONTENT_ADDED("service_lesson_content_added"),
        CREATE_LESSON_REQUEST("service_create_lesson_request"),
        NEW_LESSON_CREATED("service_new_lesson_created"),
        ADD_NEW_LOCALIZATION_REQUEST("service_add_new_localization_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>mappingId</li>
         *  <li>lessonId</li>
         * </ls>
         */
        ADD_NEW_LOCALIZATION_SUCCESS("service_add_new_localization_success"),
        BAN_CHOOSE_USER_HOURS_REQUEST("service_ban_choose_user_hours_request"),
        LIFT_BAN_USER_ID_REQUEST("service_lift_ban_user_id_request"),
        BAN_USER_ID_REQUEST("service_ban_user_id_request"),
        BAN_CHOOSE_USER_REQUEST("service_ban_choose_user_request"),
        BAN_LIFTED_SUCCESS("service_ban_lifted_success"),
        BAN_SUCCESS("service_ban_success"),
        COURSE_MAINTENANCE_TOGGLE_SUCCESS("service_course_maintenance_toggle_success"),
        COURSE_PRICE_UPDATE_REQUEST("service_course_price_update_request"),
        COURSE_PRICE_UPDATE_SUCCESS("service_course_price_update_success"),
        NEW_BOT_CREATED("service_new_bot_created"),
        BOT_CREATED_CREATOR_NOTIFICATION("service_bot_created_creator_notification"),
        CREATE_BOT_TOKEN_ONLY_REQUEST("service_create_bot_token_only_request"),
        CREATE_BOT_CREATOR_BY_ID_REQUEST("service_create_bot_creator_by_id_request"),
        CREATE_BOT_CHOOSE_CREATOR("service_create_bot_choose_creator_request"),
        NEW_COURSE_TITLE_REQUEST("service_new_course_title_request"),
        NEW_COURSE_TELEGRAM_INVOICE_REQUEST("service_telegram_invoice_request"),
        NEW_COURSE_EXTERNAL_INVOICE_REQUEST("service_external_invoice_request"),
        NEW_COURSE_CREATED("service_new_course_created"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>lessonId</li>
         * </ls>
         */
        HOMEWORK_CONTENT_REQUEST("service_homework_content_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>homeworkId</li>
         * </ls>
         */
        NEW_HOMEWORK_CREATED("service_new_homework_created"),
        DECLINE_HOMEWORK_COMMENT_REQUEST("service_decline_homework_comment_request"),
        INVOICE_IMAGE_DELETE_REQUEST("service_invoice_image_delete_request"),
        INVOICE_IMAGE_DELETED("service_invoice_image_deleted"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>status</li>
         *  <li>courseName</li>
         * </ls>
         */
        COURSE_FEEDBACK_UPDATE_SUCCESS("service_course_feedback_update_success"),
        GENERAL_BAN_LIFTED_SUCCESS("service_general_ban_lifted_success"),
        GENERAL_BAN_SUCCESS("service_general_ban_success"),
        GENERAL_POST_STARTED("service_general_post_started"),
        GENERAL_POST_ROLES_REQUEST("service_general_post_roles_request"),
        GENERAL_POST_CONTENT_REQUEST("service_general_post_content_request"),
        GET_CONTENT_REQUEST("service_get_content_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>contentId</li>
         * </ls>
         */
        GET_CONTENT_SUCCESS("service_get_content_success"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        GIVE_TAKE_COURSE_CHOOSE_ACTION("service_give_take_course_choose_action"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>targetFullName</li>
         *  <li>targetTitle</li>
         * </ls>
         */
        COURSE_TAKEN_SUCCESSFULY("service_course_taken_successfuly"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>senderTitle</li>
         *  <li>senderFullName</li>
         * </ls>
         */
        COURSE_TAKEN_NOTIFICATION("service_course_taken_notification"),
        NEW_DELAY_SET_SUCCESS("service_new_delay_set_success"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>maxDelay</li>
         * </ls>
         */
        NEW_HOMEWORK_DELAY_REQUEST("service_new_homework_delay_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>status</li>
         * </ls>
         */
        HOMEWORK_FEEDBACK_UPDATE_SUCCESS("service_homework_feedback_update_success"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>status</li>
         * </ls>
         */
        COURSE_HOMEWORK_UPDATE_SUCCESS("service_course_homework_update_success"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>mediaTypes</li>
         * </ls>
         */
        MEDIA_TYPES_UPDATE_SUCCESS("service_homework_media_types_update_success"),
        MEDIA_TYPES_REQUEST("service_homework_media_types_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>status</li>
         * </ls>
         */
        REPEATED_COMPLETION_UPDATE_SUCCESS("service_repeated_completion_update_success"),
        INVOICE_IMAGE_REQUEST("service_invoice_image_request"),
        INVOICE_IMAGE_UPDATED("service_invoice_image_updated"),
        REVIEW_COMMENT_REQUEST("service_review_comment_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>maxDelay</li>
         * </ls>
         */
        NEW_LESSON_DELAY_REQUEST("service_new_lesson_delay_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>mentorsInfo</li>
         *  <li>supportInfo</li>
         *  <li>creatorInfo</li>
         * </ls>
         */
        GET_ADMIN_LIST("service_get_admin_list"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>bots</li>
         * </ls>
         */
        LIST_BOTS("service_list_bots"),
        LOCALIZATION_FILES_REQUEST("service_localization_files_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>filesUpdated</li>
         * </ls>
         */
        LOCALIZATION_FILES_UPDATED("service_localization_files_updated"),
        POST_CONTENT_AND_ROLES_REQUEST("service_post_content_and_roles_request"),
        POST_CONTENT_REQUEST("service_post_content_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>targetFullName</li>
         *  <li>newRoleType</li>
         * </ls>
         */
        SET_ROLE_SUCCESS("service_set_role_success"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>status</li>
         * </ls>
         */
        TOGGLE_RECEIVE_HOMEWORK("service_toggle_receive_homework"),
        LOCALIZATIONS_REFRESH_SUCCESS("service_localizations_refresh_success"),
        MENU_REFRESH_SUCCESS("service_menu_refresh_success"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>spentStars</li>
         * </ls>
         */
        REFUND_CONFIRMATION_PHRASE("service_refund_confirmation_phrase"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>confirmationPhrase</li>
         * </ls>
         */
        REFUND_CONFIRMATION_REQUEST("service_refund_confirmation_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>contentId</li>
         *  <li>lessonId</li>
         * </ls>
         */
        LESSON_CONTENT_REMOVED("service_lesson_content_removed"),
        REMOVE_LESSON_CONTENT_REQUEST("service_remove_lesson_content_request"),
        DELETE_COURSE_SUCCESS("service_delete_course_success"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>confirmationPhrase</li>
         * </ls>
         */
        DELETE_COURSE_REQUEST("service_delete_course_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        DELETE_COURSE_CONFIRMATION_PHRASE("service_delete_course_confirmation_phrase"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>position</li>
         * </ls>
         */
        DELETE_LESSON_CONFIRMATION_PHRASE("service_delete_lesson_confirmation_phrase"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>confirmationPhrase</li>
         * </ls>
         */
        DELETE_LESSON_REQUEST("service_delete_lesson_request"),
        DELETE_LESSON_SUCCESS("service_delete_lesson_success"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>availableLanguageCodes</li>
         *  <li>mappingId</li>
         * </ls>
         */
        REMOVE_LOCALIZATION_FROM_MAPPING_REQUEST("service_remove_localization_from_mapping_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>languageCode</li>
         *  <li>mappingId</li>
         * </ls>
         */
        REMOVE_LOCALIZATION_FROM_MAPPING_SUCCESS("service_remove_localization_from_mapping_success"),
        REVIEW_CONTENT_REQUEST("service_review_content_request"),
        UPDATE_REVIEW_COMMENT_REQUEST("service_update_review_comment_request"),
        UPLOAD_CONTENT_REQUEST("service_upload_content_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>contentId</li>
         * </ls>
         */
        UPLOAD_CONTENT_SUCCESS("service_upload_content_success"),
        SUPPORT_REPLY_REPLY_REQUEST("service_support_reply_reply_request"),
        SUPPORT_REPLY_REPLY_SENT("service_support_reply_reply_sent"),
        SUPPORT_REQUEST_REPLY_REQUEST("service_support_request_reply_request"),
        SUPPORT_REQUEST_REPLY_SENT("service_support_request_reply_sent"),
        LANGUAGE_MANUALLY_SET("service_language_manually_set"),
        LANGUAGE_RESET_TO_DEFAULT("service_language_reset_to_default"),
        SEND_HOMEWORK_REQUEST("service_send_homework_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>targetTitle</li>
         *  <li>targetFullName</li>
         * </ls>
         */
        PRIVATE_MESSAGE_CONTENT_REQUEST("service_private_message_content_request"),
        PRIVATE_MESSAGE_USER_REQUEST("service_private_message_user_request"),
        PRIVATE_MESSAGE_SENT("service_private_message_sent"),
        SUPPORT_REQUEST_SENT("service_support_request_sent"),
        SUPPORT_REQUEST_CONTENT_REQUEST("service_support_request_content_request"),
        SET_ROLE_USER_REQUEST("service_set_role_user_request"),
        LESSON_MAPPING_ORDER_CHANGE_REQUEST("service_lesson_mapping_order_change_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>mappingId</li>
         *  <li>lessonId</li>
         *  <li>index</li>
         * </ls>
         */
        LESSON_MAPPING_ORDER_CHANGE_SUCCESS("service_lesson_mapping_order_change_success"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>newRefundStage</li>
         * </ls>
         */
        NEW_REFUND_STAGE_SUCCESS("service_new_refund_stage_success"),
        NEW_REFUND_STAGE_REQUEST("service_new_refund_stage_request"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>homeworkId</li>
         *  <li>mappingId</li>
         * </ls>
         */
        HOMEWORK_CONTENT_UPDATED("service_homework_content_updated"),
        MENU_MANUALLY_REMOVED("service_menu_manually_removed"),
        MENU_MANUALLY_REMOVED_REQUEST("service_menu_manually_removed_request"),
        MENU_MANUALLY_REMOVED_SUCCESS("service_menu_manually_removed_success"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        COURSE_COMPLETED_DEFAULT("service_course_completed_default"),
        COURSE_NEXT_STAGE_MEDIA_GROUP_BYPASS("service_course_next_stage_media_group_bypass"),
        YES,
        NO;

        private String locName;

        Service() {
            this.locName = "service_" + this.name().toLowerCase();
        }

        Service(String locName) {
            this.locName = locName;
        }

        @Override
        public String getLocName() {
            return locName;
        }

        public static record CourseCompletedUsersParams(int currentPage, int numberOfPages, long numberOfElements, String data, String courseName) {}
        public static record BannedParams(String whoBanned, String title) {}
        public static record TemporaryBannedParams(String whoBanned, int hoursUntilLift, String title) {}
        public static record RoleChangedParams(String whoChanged, RoleType newRoleType, String title) {}
        public static record BanLiftedParams(String whoLifted, String title) {}
        public static record GeneralBanLiftedParams(String whoLifted, String title) {}
        public static record TemporaryGeneralBanParams(String whoBanned, int hoursUntilLift, String title) {}
        public static record GeneralBanParams(String whoBanned, String title) {}
        public static record HoursParams(int hours) {}
        public static record ReviewInfoContentCommentParams(String userFullName, LocalDateTime basicTimestamp,
                String lastUpdateTimestamp, String courseName, int courseGrade, String usersWhoRead,
                String userWhoCommented, LocalDateTime commentedAt, long contentId, LocalDateTime advancedTimestamp) {}
        public static record ReviewInfoContentParams(String userFullName, LocalDateTime basicTimestamp,
                String lastUpdateTimestamp, String courseName, int courseGrade,
                String usersWhoRead, long contentId, LocalDateTime advancedTimestamp) {}
        public static record ReviewInfoCommentParams(String userFullName, LocalDateTime basicTimestamp,
                String lastUpdateTimestamp, String courseName, int courseGrade,
                String usersWhoRead, String userWhoCommented, LocalDateTime commentedAt) {}
        public static record ReviewInfoParams(String userFullName, LocalDateTime basicTimestamp, String lastUpdateTimestamp,
                String courseName, int courseGrade, String usersWhoRead) {}
        public static record ReviewContentUpdatedParams(String courseName) {}
        public static record ReviewCourseGradeUpdatedParams(String courseName) {}
        public static record CommentSubmittedNotificationParams(String courseName, String commenterFullName, String title) {}
        public static record BasicReviewSubmittedParams(String courseName) {}
        public static record AdvancedReviewSubmittedParams(String courseName) {}
        public static record HomeworkFeedbackRequestNotificationParams(Long targetId, String targetFullName, String targetLanguage, String courseName, int lessonIndex) {}
        public static record HomeworkDeclinedNotificationPlusCommentParams(String courseName, int lessonIndex, String whoApproved, String title) {}
        public static record HomeworkApprovedNotificationParams(String courseName, int lessonIndex, String whoApproved, String title) {}
        public static record HomeworkApprovedNotificationPlusCommentParams(String courseName, int lessonIndex, String whoApproved, String title) {}
        public static record HomeworkSubmittedNotificationParams(Long targetId, String targetFullName, String targetLanguage) {}
        public static record SupportReplyInfoParams(String userFullName, String title) {}
        public static record SupportInfoParams(String userFullName, LocalDateTime timestamp, String tag) {}
        public static record SupportRequestResolvedParams(String userFullName, String title) {}
        public static record SuccessfulPaymentParams(String courseName) {}
        public static record UserBoughtCourseParams(String userFullName, String courseName) {}
        public static record AutomaticRefundNotificationParams(long userId, long courseId) {}
        public static record AutomaticRefundParams(String courseName) {}
        public static record RefundSuccessParams(String courseName) {}
        public static record UserRefundedCourseParams(String courseName, String userFullName) {}
        public static record CourseGiftedSuccessfullyParams(String courseName, String targetFullName, String targetTitle) {}
        public static record CourseGiftedNotificationParams(String courseName, String senderTitle, String senderFullName) {}
        public static record BotStatisticsReportParams(long numberOfCourses, long coursesBought, long coursesRefunded,
                long coursesCurrentlyOwned, long totalStarsIncome, long coursesGifted,
                long numberOfUsers, long numberOfBannedUsers) {}
        public static record CourseStatisticsReportParams(String courseName, long timesBought, long timesRefunded,
                long totalStarsIncome, long numberOfOwners, long numberOfUsersWhoCompleted, long timesGifted) {}
        public static record BotUsersParams(int currentPage, int numberOfPages, long numberOfElements, String data) {}
        public static record CourseUsersParams(int currentPage, int numberOfPages, long numberOfElements, String data, String courseName) {}
        public static record CourseStageUsersParams(int currentPage, int numberOfPages, long numberOfElements, String data, String courseName, int stage) {}
        public static record PrivateMessageInfoParams(String senderFullName, String title) {}
        public static record PostCompletedParams(int successes, int failures) {}
        public static record OnMaintenanceStatusChangeParams(String status) {}
        public static record CourseFeedbackUpdateSuccessParams(String status, String courseName) {}
        public static record CourseHomeworkUpdateSuccessParams(String status, String courseName) {}
        public static record AddLessonContentRequestParams(long lessonId) {}
        public static record LessonContentAddedParams(long mappingId, long lessonId) {}
        public static record AddNewLocalizationSuccessParams(long mappingId, long contentId) {}
        public static record AddNewLocalizationRequestParams(long mappingId) {}
        public static record CourseMaintenanceToggleSuccessParams(String status, String courseName) {}
        public static record CoursePriceUpdateSuccessParams(String courseName, int currentPrice) {}
        public static record CoursePriceUpdateRequestParams(String courseName, int currentPrice) {}
        public static record HomeworkContentRequestParams(long lessonId) {}
        public static record NewHomeworkCreatedParams(long homeworkId) {}
        public static record GetContentSuccessParams(long contentId) {}
        public static record GiveTakeCourseChooseActionParams(String courseName) {}
        public static record CourseTakenSuccessfullyParams(String courseName, String targetFullName, String targetTitle) {}
        public static record CourseTakenNotificationParams(String courseName, String senderTitle, String senderFullName) {}
        public static record HomeworkFeedbackUpdateSuccessParams(String status) {}
        public static record RepeatedCompletionUpdateSuccessParams(String status) {}
        public static record MediaTypesUpdateSuccessParams(String mediaTypes) {}
        public static record NewHomeworkDelayRequestParams(int maxDelay) {}
        public static record NewLessonDelayRequestParams(int maxDelay) {}
        public static record GetAdminListParams(String mentorsInfo, String supportInfo, String creatorInfo) {}
        public static record ListBotsParams(String bots) {}
        public static record LocalizationFilesUpdatedParams(int filesUpdated) {}
        public static record SetRoleSuccessParams(String targetFullName, RoleType newRoleType) {}
        public static record ToggleReceiveHomeworkParams(String status) {}
        public static record RefundConfirmationPhraseParams(String courseName, int spentStars) {}
        public static record RefundConfirmationRequestParams(String confirmationPhrase) {}
        public static record LessonContentRemovedParams(long mappingId, long lessonId) {}
        public static record DeleteCourseRequestParams(String confirmationPhrase) {}
        public static record DeleteLessonRequestParams(String confirmationPhrase) {}
        public static record DeleteCourseConfirmationPhraseParams(String courseName) {}
        public static record DeleteLessonConfirmationPhraseParams(String courseName, int position) {}
        public static record RemoveLocalizationFromMappingRequestParams(long mappingId, String availableLanguageCodes) {}
        public static record RemoveLocalizationFromMappingSuccessParams(long mappingId, String languageCode) {}
        public static record UploadContentSuccessParams(long mappingId) {}
        public static record PrivateMessageContentRequestParams(String targetTitle, String targetFullName) {}
        public static record LessonMappingOrderChangeSuccessParams(long mappingId, long lessonId, int index) {}
        public static record NewRefundStageRequestParams(String courseName) {}
        public static record NewRefundStageSuccessParams(String courseName, String newRefundStage) {}
        public static record HomeworkContentUpdatedParams(long homeworkId, long mappingId) {}
        public static record CourseCompletedDefaultParams(String courseName) {}
    }
    
    public static enum Error implements LocalizationKey {
        /**
         * Possible parameters:
         * <ls>
         *  <li>messageIndex</li>
         * </ls>
         */
        MESSAGE_TEXT_MISSING("error_message_text_missing"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>providedMessagesNumber</li>
         *  <li>expectedMessagesNumber</li>
         * </ls>
         */
        NUMBER_OF_MESSAGES("error_number_of_messages"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>providedMessagesNumber</li>
         *  <li>expectedMessagesNumber</li>
         * </ls>
         */
        AT_LEAST_NUMBER_OF_MESSAGES("error_at_least_number_of_messages"),
        USER_IS_BANNED_IN_BOT("error_user_is_banned_in_bot"),
        USER_NOT_REGISTRED("error_user_not_registred"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>excMessage</li>
         *  <li>excClassName</li>
         * </ls>
         */
        NO_EXCEPTION_LOCALIZATION_AVAILABLE("error_no_exception_localization_available"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>excMessage</li>
         * </ls>
         */
        TELEGRAM_INTERNAL("error_telegram_internal"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>excMessage</li>
         *  <li>excClassName</li>
         * </ls>
         */
        UNSPECIFIED_EXCEPTION("error_unspecified_exception"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>excMessage</li>
         *  <li>excClassName</li>
         *  <li>userId</li>
         *  <li>botId</li>
         * </ls>
         */
        CRITICAL_DIRECTOR_NOTIFICATION("error_critical_director_notification"),
        COURSE_NOT_FOUND("error_course_not_found"),
        LESSON_NOT_FOUND("error_lesson_not_found"),
        HOMEWORK_NOT_FOUND("error_homework_not_found"),
        CONTENT_NOT_FOUND("error_content_not_found"),
        COURSE_PROGRESS_NOT_FOUND("error_course_progress_not_found"),
        HOMEWORK_PROGRESS_NOT_FOUND("error_homework_progress_not_found"),
        USER_NOT_FOUND("error_user_not_found"),
        CONTENT_MAPPING_NOT_FOUND("error_content_mapping_not_found"),
        REVIEW_NOT_FOUND("error_review_not_found"),
        BOT_ROLE_NOT_FOUND("error_bot_role_not_found"),
        SUPPORT_REPLY_NOT_FOUND("error_support_reply_not_found"),
        SUPPORT_REQUEST_NOT_FOUND("error_support_request_not_found"),
        BOT_VISIBILITY_MISMATCH("error_bot_visibility_mismatch"),
        UNAVAILABLE_IN_REGULAR_BOT("error_unavailable_in_regular_bot"),
        COURSE_OWNERSHIP_NOT_FOUND("error_course_ownership_not_found"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>sentContentMediaType</li>
         *  <li>allowedMediaTypes</li>
         * </ls>
         */
        CONTENT_MEDIA_GROUP_DOES_NOT_MATCH("error_content_media_group_does_not_match"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>missingAuthorities</li>
         * </ls>
         */
        ACCESS_DENIED("error_access_denied"),
        CONTENT_TEXT_AND_CAPTIONS("error_content_text_and_captions"),
        CONTENT_SEVERAL_TEXT("error_content_several_text"),
        CONTENT_AUDIO_GROUP_FAILURE("error_content_audio_group_failure"),
        CONTENT_DOCUMENT_GROUP_FAILURE("error_content_document_group_failure"),
        CONTENT_GRAPHICS_GROUP_FAILURE("error_content_graphics_group_failure"),
        COURSE_UNDER_MAINTENANCE("error_course_under_maintenance"),
        UNKNOWN_MEDIA_TYPE("error_unknown_media_type"),
        MAPPING_NOT_IN_LESSON("error_mapping_not_in_lesson"),
        UPDATE_MESSAGE_FAILURE("error_update_message_failure"),
        MENU_NOT_FOUND("error_menu_not_found"),
        MULTIPAGE_LIST_META_NOT_FOUND("error_multipage_list_meta_not_found"),
        NO_DATA_FOR_MULTIPAGE_LIST("error_no_data_for_multipage_list"),
        MTG_NOT_FOUND("error_mtg_not_found"),
        USER_IS_NOT_BANNED("error_user_is_not_banned"),
        USER_ALREADY_BANNED("error_user_already_banned"),
        CANNOT_SET_BANNED_ROLE("error_cannot_set_banned_role"),
        CREATOR_BAN("error_creator_ban"),
        SELF_BAN("error_self_ban"),
        SAME_ROLE("error_same_role"),
        PREDEFINED_CHANGE_ROLES("error_predefined_change_roles"),
        SELF_CHANGE_ROLE("error_self_change_role"),
        CREATOR_CHANGE_ROLE("error_creator_change_role"),
        DIRECTOR_CHANGE_ROLE("error_director_change_role"),
        FILE_NOT_LOCALIZATION("error_file_not_localization"),
        SEND_FILE_FAILURE("error_send_file_failure"),
        COMMIT_BASIC_REVIEW_FAILURE("error_commit_basic_review_failure"),
        REVIEW_ALREADY_PRESENT("error_review_already_present"),
        NO_ARCHIVE_REVIEWS_AVAILABLE("error_no_archive_reviews_available"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>timeLeft</li>
         * </ls>
         */
        AWAITING_LESSON("error_awaiting_lesson"),
        NO_CONTENT_IN_LESSON("error_no_content_in_lesson"),
        SELECT_LESSON_COURSE_NOT_COMPLETED("error_select_lesson_course_not_completed"),
        HOMEWORK_ALREADY_COMPLETED("error_homework_already_completed"),
        HOMEWORK_ALREADY_AWAITS_APPROVAL("error_homework_already_awaits_approval"),
        NO_SUPPORT_REQUESTS_AVAILABLE_FOR_USER("error_no_support_requests_available_for_user"),
        SEND_INVOICE_FAILURE("error_send_invoice_failure"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>currentPrice</li>
         * </ls>
         */
        PRE_CHECKOUT_PRICE_MISMATCH("error_pre_checkout_price_mismatch"),
        PRE_CHECKOUT_CURRENCY_MISMATCH("error_pre_checkout_currency_mismatch"),
        PRE_CHECKOUT_UNKNOWN_COURSE("error_pre_checkout_unknown_course"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        PRE_CHECKOUT_COURSE_ALREADY_OWNED("error_pre_checkout_course_already_owned"),
        ANSWER_PRECHECKOUT_FAILURE("error_answer_precheckout_failure"),
        PAYMENT_SUCCESS_SERVER_ON_MAINTENANCE("error_payment_success_server_on_maintenance"),
        REFUND_FAILURE("error_refund_failure"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        AUTOMATIC_REFUND_FAILURE("error_automatic_refund_failure"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseId</li>
         *  <li>userId</li>
         * </ls>
         */
        AUTOMATIC_REFUND_FAILURE_NOTIFICATION("error_automatic_refund_failure_notification"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>targetFullName</li>
         *  <li>targetTitle</li>
         * </ls>
         */
        GIVE_COURSE_ALREADY_OWNED("error_give_course_already_owned"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>maxRefundStage</li>
         *  <li>currentStage</li>
         * </ls>
         */
        REFUND_USER_ADVANCED_TOO_FAR("error_refund_user_advanced_too_far"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        REFUND_COURSE_NOT_OWNED("error_refund_course_not_owned"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        REFUND_INVALID_SOURCE("error_refund_invalid_source"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        REFUND_COURSE_COMPLETED("error_refund_course_completed"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         * </ls>
         */
        REFUND_COURSE_UNAVAILABLE("error_refund_course_unavailable"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>maxNumberOfDays</li>
         *  <li>currentNumberOfDays</li>
         * </ls>
         */
        REFUND_PURCHASE_TOO_OLD("error_refund_purchase_too_old"),
        REFUND_ENTITY_NOT_FOUND("error_refund_entity_not_found"),
        SERVER_ON_MAINTENANCE("error_server_on_maintenance"),
        BOTLORD_CALLBACK_EXCEPTION("error_botlord_callback_exception"),
        COMMIT_ADVANCED_REVIEW_FAILURE("error_commit_advanced_review_failure"),
        LEAVE_COMMENT_FAILURE("error_leave_comment_failure"),
        UPDATE_COMMENT_FAILURE("error_update_comment_failure"),
        UPDATE_COMMENT_FORBIDDEN("error_update_comment_forbidden"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseGrade</li>
         * </ls>
         */
        SAME_NEW_COURSE_GRADE("error_same_new_course_grade"),
        UPDATE_CONTENT_NOT_PRESENT("error_update_content_not_present"),
        SESSION_EXPIRED("error_session_expired"),
        MIXED_SESSIONS("error_mixed_sessions"),
        SESSION_NO_SHARED_ENTITY("error_session_no_shared_entity"),
        MORE_THEN_ONE_SESSION("error_more_then_one_session"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>userFullName</li>
         *  <li>title</li>
         * </ls>
         */
        SUPPORT_REQUEST_ALREADY_ANSWERED("error_support_request_already_answered"),
        SUPPORT_REQUEST_ALREADY_RESOLVED("error_support_request_already_resolved"),
        REPLY_ALREADY_ANSWERED("error_reply_already_answered"),
        USER_NOT_ELIGIBLE_FOR_SUPPORT("error_user_not_eligible_for_support"),
        SUPPORT_STAFF_REQUEST("error_support_staff_request"),
        SEND_CONTENT("error_send_content"),
        PRIVATE_MESSAGE_USER_NOT_REGISTERED_IN_BOT("error_private_message_user_not_registered_in_bot"),
        TOO_MANY_POST_REQUESTS("error_too_many_post_requests"),
        POST_NO_ROLES("error_post_no_roles"),
        LOCALIZATION_PARAMS_INVALID("error_localization_params_invalid"),
        LOCALIZATION_DOES_NOT_EXIST("error_localization_does_not_exist"),
        IS_REFRESHING("error_is_refreshing"),
        PARSE_ID_FAILURE("error_parse_id_failure"),
        PARSE_ID_BOUNDS_FAILURE("error_parse_int_bounds_failure"),
        PARSE_INT_FAILURE("error_parse_int_failure"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>lowerBound</li>
         *  <li>upperBound</li>
         * </ls>
         */
        PARSE_INT_BOUNDS_FAILURE("error_parse_int_bounds_failure"),
        TEXT_BOUNDS_FAILURE("error_text_bounds_failure"),
        INVALID_START_PARAM("error_invalid_start_param"),
        TEXT_MESSAGE_EXPECTED("error_text_message_expected"),
        FAILED_ADVANCE_TO_NEXT_LESSON("error_failed_advance_to_next_lesson"),
        STALE_MENU("error_stale_menu"),
        LANGUAGE_CODE_LENGTH("error_language_code_length"),
        LESSON_POSITION_INVALID("error_lesson_position_invalid"),
        PARSE_INDEX_FAILURE("error_parse_index_failure"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>mappingId</li>
         *  <li>languageCode</li>
         * </ls>
         */
        LOCALIZED_CONTENT_IS_ALREADY_PRESENT("error_localized_content_is_already_present"),
        BOT_TOKEN_PATTERN_MISMATCH("error_bot_token_pattern_mismatch"),
        BOT_ALREADY_EXISTS("error_bot_already_exists"),
        INVOICE_IMAGE_DOES_NOT_EXIST("error_invoice_image_does_not_exist"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>courseName</li>
         *  <li>targetFullName</li>
         *  <li>targetTitle</li>
         * </ls>
         */
        TAKE_COURSE_BOUGHT("error_take_course_bought"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>availableMediaTypes</li>
         * </ls>
         */
        PARSE_MEDIA_TYPES_FAILURE("error_parse_media_types_failure"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>availableRoleTypes</li>
         * </ls>
         */
        PARSE_ROLE_TYPES_FAILURE("error_parse_role_types_failure"),
        DOWNLOAD_FILE("error_download_file"),
        NO_MENTORS("error_no_mentors"),
        NO_SUPPORT_STAFF("error_no_support_staff"),
        LOCALIZATIONS_KEY_PARSE_FAILURE("error_localizations_key_parse_failure"),
        LANGUAGE_CODE_REQUIRED("error_language_code_required"),
        MAINTENANCE_IN_NOT_ENABLED("error_maintenance_in_not_enabled"),
        REFUND_CONFIRMATION_PHRASE_FAILURE("error_refund_confirmation_phrase_failure"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>numberOfOwnerships</li>
         * </ls>
         */
        DELETE_COURSE_ACTIVE_OWNERSHIPS("error_delete_course_active_ownerships"),
        DELETE_COURSE_CONFIRMATION_PHRASE_FAILURE("error_delete_course_confirmation_phrase_failure"),
        DELETE_LESSON_CONFIRMATION_PHRASE_FAILURE("error_delete_lesson_confirmation_phrase_failure"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>languageCode</li>
         *  <li>availableLanguageCodes</li>
         * </ls>
         */
        NO_LOCALIZATIONS_DELETED("error_no_localizations_deleted"),
        SAME_CONTENT_POSITION("error_same_content_position"),
        SAME_NEW_REFUND_STAGE("error_same_new_refund_stage"),
        MENU_SNAPSHOT_NOT_FOUND("error_menu_snapshot_not_found"),
        MENU_MANUALLY_REMOVED_FAILED("error_menu_manually_removed_failed"),
        NO_COURSES("error_no_courses"),
        PARSE_URL_FAILURE("error_parse_url_failure"),
        PARSE_URL_FAILURE_INVALID_SCHEME("error_parse_url_failure_invalid_scheme"),
        COURSE_PRICE_UPDATE_EXTERNAL_INVOICE("error_course_price_update_external_invoice"),
        COURSE_REFUND_STAGE_UPDATE_EXTERNAL_INVOICE("error_course_refund_stage_update_external_invoice"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>maxRefundStage</li>
         * </ls>
         */
        REFUND_STAGE_GREATER_THAN_NUMBER_OF_LESSONS("error_refund_stage_greater_than_number_of_lessons"),
        COURSE_VALIDATION_NO_LESSONS("error_course_validation_no_lessons"),
        /**
         * Possible parameters:
         * <ls>
         *  <li>lessonIndex</li>
         * </ls>
         */
        COURSE_VALIDATION_NO_CONTENT_IN_LESSON("error_course_validation_no_content_in_lesson"),
        DIRECTOR_BAN("error_director_ban"),
        SEND_MESSAGE,
        POST_REQUEST_FAILURE;
        
        private String locName;

        Error() {
            this.locName = "error_" + this.name().toLowerCase();
        }

        Error(String locName) {
            this.locName = locName;
        }

        @Override
        public String getLocName() {
            return locName;
        }

        public static record MessageTextMissingParams(int messageIndex) {}
        public static record NumberOfMessagesParams(int providedMessagesNumber, int expectedMessagesNumber) {}
        public static record AtLeastNumberOfMessagesParams(int providedMessagesNumber, int expectedMessagesNumber) {}
        public static record NoExceptionLocalizationAvailableParams(String excMessage, String excClassName) {}
        public static record TelegramInternalParams(String excMessage) {}
        public static record UnspecifiedExceptionParams(String excMessage, String excClassName) {}
        public static record CriticalDirectorNotificationParams(String excMessage, String excClassName, long userId, long botId) {}
        public static record ContentMediaGroupDoesNotMatchParams(MediaType sentContentMediaType, List<MediaType> allowedMediaTypes) {}
        public static record AccessDeniedParams(List<AuthorityType> missingAuthorities) {}
        public static record AwaitingLessonParams(String timeLeft) {}
        public static record PreCheckoutPriceMismatchParams(int currentPrice) {}
        public static record PreCheckoutCourseAlreadyOwnedParams(String courseName) {}
        public static record AutomaticRefundFailureParams(String courseName) {}
        public static record AutomaticRefundFailureNotificationParams(long userId, long courseId) {}
        public static record GiveCourseAlreadyOwnedParams(String courseName, String targetFullName, String targetTitle) {}
        public static record RefundUserAdvancedTooFarParams(String courseName, int maxRefundStage, int currentStage) {}
        public static record RefundCourseNotOwnedParams(String courseName) {}
        public static record RefundInvalidSourceParams(String courseName) {}
        public static record RefundCourseCompletedParams(String courseName) {}
        public static record RefundCourseUnavailableParams(String courseName) {}
        public static record RefundPurchaseTooOldParams(String courseName, int maxNumberOfDays, long currentNumberOfDays) {}
        public static record SameNewCourseGradeParams(int courseGrade) {}
        public static record SupportRequestAlreadyAnsweredParams(String userFullName, String title) {}
        public static record ParseIntBoundsFailureParams(int lowerBound, int upperBound) {}
        public static record LocalizedContentIsAlreadyPresentParams(long mappingId, String languageCode) {}
        public static record TextBoundsFailureParams(int lowerBound, int upperBound) {}
        public static record TakeCourseBoughtParams(String courseName, String targetFullName, String targetTitle) {}
        public static record ParseMediaTypesFailureParams(String availableMediaTypes) {}
        public static record ParseRoleTypesFailureParams(String availableRoleTypes) {}
        public static record DeleteCourseActiveOwnershipsParams(long numberOfOwnerships) {}
        public static record NoLocalizationsDeletedParams(String languageCode, String availableLanguageCodes) {}
        public static record RefundStageGreaterThanNumberOfLessonsParams(int maxRefundStage) {}
        public static record CourseValidationNoContentInLessonParams(int lessonIndex) {}
        public static record PostRequestFailureParams(int successes, int failures) {}
    }
}
