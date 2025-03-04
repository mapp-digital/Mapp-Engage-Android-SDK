package com.appoxee.internal.ui.push.model

enum class CategoryType(val categoryName: String) {
    APX_YES_NO_OPEN("apx_yes_no_open"),
    APX_YES_NO_DISMISS("apx_yes_no_dismiss"),
    APX_ACC_DEC_OPEN("apx_acc_dec_open"),
    APX_ACC_DEC_DISMISS("apx_acc_dec_dismiss"),
    APX_PLAY_OPEN("apx_play_open"),
    APX_BUY_OPEN("apx_buy_open"),
    APX_FOLLOW_DISMISS("apx_follow_dismiss"),
    APX_REMIND_ME_DISMISS("apx_remind_me_dismiss"),
    APX_REDEEM_OPEN("apx_redeem_open"),
    APX_READ_OPEN("apx_read_open"),
    APX_ACCEPT_NOTIFICATION_SETTINGS_OPEN("apx_accept_notification_setings_open"),
    APX_YES_NOTIFICATION_SETTINGS_OPEN("apx_yes_notification_setings_open"),
    APX_CUSTOM_PUSH_1("apx_custom_push_1"),
    APX_FULL_CUSTOM_PUSH("full_custom_push_1"),
    APX_SHOP_REMIND_OPEN("apx_shop_remind_open"),
    APX_SPECIFIC_ANDROID("apx_specific_android");

    companion object {
        @JvmStatic
        fun fromString(value: String): CategoryType? {
            return try {
                CategoryType.entries.firstOrNull { value == it.categoryName }
            } catch (e: Exception) {
                null
            }
        }
    }
}