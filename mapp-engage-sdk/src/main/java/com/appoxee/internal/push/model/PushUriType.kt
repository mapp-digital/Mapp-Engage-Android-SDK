package com.appoxee.internal.push.model

import com.appoxee.internal.model.request.events.PushAction

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
        internal fun PushUriType?.toPushAction(): PushAction {
            return when (this) {
                KEY_URL -> PushAction.OPEN_LANDING_PAGE
                KEY_APP_PACKAGE -> PushAction.OPEN_STORE
                KEY_DEEP_LINK -> PushAction.OPEN_DEEP_LINK
                KEY_DIALER -> PushAction.OPEN_DIALER
                else -> PushAction.LAUNCH_APP
            }
        }
    }
}