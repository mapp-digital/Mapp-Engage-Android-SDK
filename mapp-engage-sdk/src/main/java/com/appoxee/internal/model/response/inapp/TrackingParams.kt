package com.appoxee.internal.model.response.inapp

data class TrackingParams(
    var reason: String? = null,
    var timeSinceLastDisplay: Long? = null,
    var link: String? = null
) {

    companion object {
        //Reasons for dismissal or webview not loaded in Android.
        const val REASON_TIMEOUT_EXPIRATION: String = "timeout_expiration"
        const val REASON_WEB_VIEW_LOAD_ERROR: String = "webview_load_error"
        const val REASON_CONTENT_LOAD_ERROR: String = "content_load_error"
        const val REASON_USER_DISMISSED: String = "user_dismissed"
        const val REASON_CONTENT_LOAD_TIMEOUT: String = "content_load_timeout"
        const val REASON_OTHER_MESSAGE_DISPLAYING: String = "other_message_displaying"
        const val REASON_SESSION_INTERRUPTED_ERROR: String = "session_interrupted_error"

    }

    fun toMap(): Map<String, *> {
        val result = mutableMapOf<String, Any>()
        reason?.let {
            result.put("reason", it)
        }
        timeSinceLastDisplay?.let {
            result.put("time_since_display_millis", it)
        }
        link?.let {
            result.put("link", it)
        }

        return result
    }
}
