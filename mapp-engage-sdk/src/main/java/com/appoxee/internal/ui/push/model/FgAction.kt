package com.appoxee.internal.ui.push.model

import android.os.Parcelable
import com.appoxee.internal.util.getNullableString
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
internal data class FgAction(
    val apxUrl: String? = null,
    val apxAid: String? = null,
    val apxVc: String? = null,
    val apxInbox: String? = null,
    val apxUrlInternal: String? = null,
    val apxDpl: String? = null,
    val isDestructive: Boolean = false,
) : Parcelable {

    fun isDestroyAction(): Boolean {
        return isDestructive &&
                apxUrl.isNullOrEmpty() &&
                apxAid.isNullOrEmpty() &&
                apxVc.isNullOrEmpty() &&
                apxInbox.isNullOrEmpty() &&
                apxUrlInternal.isNullOrEmpty() &&
                apxDpl.isNullOrEmpty()
    }

    @Synchronized
    fun getUriType(): PushUriType {
        if (!apxUrl.isNullOrEmpty()) return PushUriType.KEY_URL
        if (!apxAid.isNullOrEmpty()) return PushUriType.KEY_APP_PACKAGE
        if (!apxVc.isNullOrEmpty()) return PushUriType.KEY_APX_VC
        if (!apxInbox.isNullOrEmpty()) return PushUriType.KEY_INBOX
        if (!apxUrlInternal.isNullOrEmpty()) return PushUriType.KEY_URL_INTERNAL
        if (!apxDpl.isNullOrEmpty()) {
            return if (apxDpl.startsWith("tel:"))
                PushUriType.KEY_DIALER
            else
                PushUriType.KEY_DEEP_LINK
        }
        return if (isDestructive) PushUriType.KEY_APP_DESTROY_PUSH else PushUriType.KEY_LAUNCH_APP
    }

    @Synchronized
    fun getAction(): String {
        return when (getUriType()) {
            PushUriType.KEY_URL -> apxUrl!!
            PushUriType.KEY_APP_PACKAGE -> apxAid!!
            PushUriType.KEY_APX_VC -> apxVc!!
            PushUriType.KEY_INBOX -> apxInbox!!
            PushUriType.KEY_URL_INTERNAL -> apxUrlInternal!!
            PushUriType.KEY_DEEP_LINK -> apxDpl!!
            PushUriType.KEY_DIALER -> apxDpl!!
            PushUriType.KEY_LAUNCH_APP -> PushUriType.KEY_LAUNCH_APP.value
            else -> PushUriType.KEY_APP_DESTROY_PUSH.value
        }
    }

    companion object {
        private const val APX_URL = "apx_url"
        private const val APX_AID = "apx_aid"
        private const val APX_VC = "apx_vc"
        private const val APX_INBOX = "apx_inbox"
        private const val APX_URL_INTERNAL = "apx_url_internal"
        private const val APX_DPL = "apx_dpl"
        fun fromJSON(json: JSONObject, isDestructive: Boolean = false): FgAction {
            return FgAction(
                apxUrl = json.getNullableString(APX_URL),
                apxAid = json.getNullableString(APX_AID),
                apxVc = json.getNullableString(APX_VC),
                apxInbox = json.getNullableString(APX_INBOX),
                apxUrlInternal = json.getNullableString(APX_URL_INTERNAL),
                apxDpl = json.getNullableString(APX_DPL),
                isDestructive = isDestructive
            )
        }
    }
}