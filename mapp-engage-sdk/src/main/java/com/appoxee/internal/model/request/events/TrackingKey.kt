package com.appoxee.internal.model.request.events

internal enum class TrackingKey(val key: String) {
    IA_MSG_DISPLAYED("ia_message_displayed"),
    IA_MSG_DISMISSED("ia_message_dismissal"),
    IA_MSG_APP_STORE("ia_message_app_store"),
    IA_MSG_NOT_DISPLAYED("ia_message_not_displayed"),
    IA_MSG_CUSTOM_ACTION("ia_message_custom_action"),
    IA_MSG_DEEPLINK("ia_message_deep_link"),
    IA_MSG_LANDING_PAGE_INTERNAL("ia_message_landing_page_internal"),
    IA_MSG_LANDING_PAGE_EXTERNAL("ia_message_landing_page_external"),
    IA_MSG_DIAL_NUMBER("ia_message_dial_number"),

    INBOX_MESSAGE_UNREAD("inbox_message_unread"),
    INBOX_MESSAGE_READ("inbox_message_read"),
    INBOX_MESSAGE_DELETED("inbox_message_deleted"),

}