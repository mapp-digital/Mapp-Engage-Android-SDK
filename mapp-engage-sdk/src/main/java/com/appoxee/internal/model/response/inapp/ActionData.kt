package com.appoxee.internal.model.response.inapp

import com.appoxee.internal.model.request.events.TrackingKey

data class ActionData(
    val link: String?,
    val openInApp: Boolean,
    val actionType: InappActionType?,
    val scheme: String? = null,
    val messageId:Long,
) {

    internal fun toTrackingKey(): TrackingKey {
        return actionType?.let { actionType ->
            when (actionType) {
                InappActionType.DEEPLINK -> TrackingKey.IA_MSG_DEEPLINK
                InappActionType.APP_STORE -> TrackingKey.IA_MSG_APP_STORE
                InappActionType.DIALER -> TrackingKey.IA_MSG_DIAL_NUMBER
                InappActionType.LANDING_PAGE -> {
                    if (openInApp) TrackingKey.IA_MSG_LANDING_PAGE_INTERNAL
                    else TrackingKey.IA_MSG_LANDING_PAGE_EXTERNAL
                }
            }
        } ?: TrackingKey.IA_MSG_NOT_DISPLAYED
    }
}
