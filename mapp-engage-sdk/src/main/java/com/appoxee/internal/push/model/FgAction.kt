package com.appoxee.internal.push.model

import com.appoxee.internal.util.getNullableString
import org.json.JSONObject

internal data class FgAction(
    val apxUrl: String? = null,
    val apxAid: String? = null,
    val apxVc: String? = null,
    val apxInbox: String? = null,
    val apxUrlInternal: String? = null,
    val apxDpl: String? = null,
    val apxDestroyPush: String? = null
) {
    fun isDestroyAction(): Boolean {
        return apxUrl == null && apxAid == null && apxVc == null && apxInbox == null && apxUrlInternal == null && apxDpl == null && apxDestroyPush == null
    }

    companion object {
        private const val APX_URL = "apx_url"
        private const val APX_AID = "apx_aid"
        private const val APX_VC = "apx_vc"
        private const val APX_INBOX = "apx_inbox"
        private const val APX_URL_INTERNAL = "apx_url_internal"
        private const val APX_DPL = "apx_dpl"
        private const val APX_DESTROY_PUSH = "apx_destroy_push"
        fun fromJSON(json: JSONObject): FgAction {
            return FgAction(
                apxUrl = json.getNullableString(APX_URL),
                apxAid = json.getNullableString(APX_AID),
                apxVc = json.getNullableString(APX_VC),
                apxInbox = json.getNullableString(APX_INBOX),
                apxUrlInternal = json.getNullableString(APX_URL_INTERNAL),
                apxDpl = json.getNullableString(APX_DPL),
                apxDestroyPush = json.getNullableString(APX_DESTROY_PUSH),
            )
        }
    }
}