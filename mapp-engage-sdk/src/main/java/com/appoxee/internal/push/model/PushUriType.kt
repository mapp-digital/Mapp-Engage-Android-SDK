package com.appoxee.internal.push.model

import com.appoxee.internal.model.request.events.ClickType

enum class PushUriType(val value: String) {
    KEY_URL("apx_url"),
    KEY_APP_PACKAGE("apx_aid"),
    KEY_APX_VC("apx_vc"),
    KEY_INBOX("apx_inbox"),
    KEY_URL_INTERNAL("apx_url_internal"),
    KEY_DEEP_LINK("apx_dpl"),
    KEY_DIALER("tel:"),
    KEY_APP_DESTROY_PUSH("push_destroy"),
    KEY_PLAY("play"),
    KEY_TURN_OFF("turn_off");

    companion object {
        internal fun PushUriType?.toPushAction(): ClickType {
            return when (this) {
                KEY_URL -> ClickType.OPEN_LANDING_PAGE
                KEY_APP_PACKAGE -> ClickType.OPEN_STORE
                KEY_DEEP_LINK -> ClickType.OPEN_DEEP_LINK
                KEY_DIALER -> ClickType.OPEN_DIALER
                KEY_PLAY -> ClickType.OPEN_RICH_PUSH
                KEY_TURN_OFF -> ClickType.DISMISS
                else -> ClickType.LAUNCH_APP
            }
        }
    }
}