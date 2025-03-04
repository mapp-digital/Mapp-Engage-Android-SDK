package com.appoxee.internal.ui.push.model

import android.net.Uri
import android.os.Parcelable
import com.appoxee.internal.model.response.Category
import com.google.firebase.messaging.RemoteMessage
import kotlinx.parcelize.Parcelize
import org.json.JSONArray

@Parcelize
internal data class PushData(
    val id: Long,
    val title: String? = null,
    val alert: String? = null,
    val bigText: String? = null,
    val sound: String? = null,
    val actionUri: Uri? = null,
    val internalUriType: String? = null,
    val collapseKey: String? = null,
    val type: String? = null,
    val userId: String? = null,
    val customerId: String? = null,
    val iosApxMedia: String? = null,
    val badgeNumber: Int? = null,
    val contentAvailable: Boolean = false,
    val silentType: String? = null,
    val silentData: String? = null,
    val buttonList: List<PushButton?> = emptyList(),
    val sendoutId: Long? = null,
    var extraFields: Map<String, String> = emptyMap(),
    val category: Category? = null,
    val language: String? = null,
    val priority: Int? = null,
) : Parcelable {

    fun getContentUriType(): PushUriType {
        return if (internalUriType.isNullOrEmpty()) {
            PushUriType.KEY_LAUNCH_APP
        } else {
            when (internalUriType) {
                PushUriType.KEY_APP_PACKAGE.value -> PushUriType.KEY_APP_PACKAGE
                PushUriType.KEY_DEEP_LINK.value -> {
                    if (actionUri?.toString()?.startsWith(PushUriType.KEY_DIALER.value) == true)
                        PushUriType.KEY_DIALER
                    else
                        PushUriType.KEY_DEEP_LINK
                }

                PushUriType.KEY_URL.value -> PushUriType.KEY_URL
                else -> PushUriType.KEY_LAUNCH_APP
            }
        }
    }

    internal companion object {
        private const val BUTTONS = "buttons"
        private const val KEY_APP_DESTROY_PUSH = "push_destroy"
        private const val KEY_MESSAGE_ID = "p"
        private const val KEY_TITLE = "push_title"
        private const val KEY_ALERT = "alert"
        private const val KEY_BIG_TEXT_BODY = "big_text_body"
        private const val KEY_BADGE_NUMBER = "badge"
        private const val KEY_MEDIA_URL = "ios_apx_media"
        private const val KEY_MEDIA_TYPE = "type"
        private const val KEY_SOUND = "sound"
        private const val KEY_COLLAPSE_KEY = "collapse_key"
        private const val KEY_APP_PACKAGE = "apx_aid"
        private const val KEY_DEEP_LINK = "apx_dpl"

        const val KEY_APX_VC = "apx_vc"
        private const val KEY_URL = "apx_url"
        private const val KEY_INBOX = "apx_inbox"
        private const val KEY_URL_INTERNAL = "apx_url_internal"
        private const val CATEGORY = "category"
        private const val CONTENT_AVAILABLE = "content_available"
        private const val SILENT_TYPE = "silent_type"
        private const val SILENT_DATA = "silent_data"
        private const val LANGUAGE = "lc"
        private const val KEY_CUSTOMER_ID = "customer_id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SENDOUT_ID = "sendout_id"
        private const val PRIORITY = "priority"

        @JvmStatic
        internal fun RemoteMessage.toPushData(categories: List<Category>): PushData {
            val map = mutableMapOf<String, String?>().apply {
                putAll(this@toPushData.data)
            }
            val uriType = getUriType(map)
            val actionUriPath = getPushOpenUriString(map)
            val categoryName = map.getData(CATEGORY)
            val category = categoryName?.let { catName ->
                categories.firstOrNull { catName == it.categoryType?.categoryName }
            }

            return PushData(
                id = map.getData(KEY_MESSAGE_ID)?.toLongOrNull() ?: 0L,
                title = map.getData(KEY_TITLE),
                alert = map.getData(KEY_ALERT),
                bigText = map.getData(KEY_BIG_TEXT_BODY),
                sound = map.getData(KEY_SOUND),
                actionUri = if (actionUriPath != null) Uri.parse(actionUriPath) else null,
                internalUriType = uriType,
                collapseKey = map.getData(KEY_COLLAPSE_KEY),
                type = map.getData(KEY_MEDIA_TYPE),
                iosApxMedia = map.getData(KEY_MEDIA_URL),
                badgeNumber = map.getData(KEY_BADGE_NUMBER)?.toIntOrNull(),
                contentAvailable = map.getData(CONTENT_AVAILABLE).toBoolean(),
                silentType = map.getData(SILENT_TYPE),
                silentData = map.getData(SILENT_DATA),
                category = category,
                language = map.getData(LANGUAGE),
                userId = map.getData(KEY_USER_ID),
                customerId = map.getData(KEY_CUSTOMER_ID),
                sendoutId = map.getData(KEY_SENDOUT_ID)?.toLongOrNull(),
                buttonList = getButtons(map, category),
                extraFields = getExtraFields(map),
                priority = this.priority
            )
        }

        private fun getButtons(
            map: MutableMap<String, String?>,
            category: Category?
        ): List<PushButton> {
            val array = map.getData(BUTTONS)?.let { JSONArray(it) }
            return if (array != null) PushButton.fromJSON(array, category) else emptyList()
        }

        private fun getExtraFields(map: MutableMap<String, String?>): Map<String, String> {
            val data = mutableMapOf<String, String>()
            map.entries.forEach {
                if (it.value != null) {
                    data[it.key] = it.value!!
                }
            }
            return data
        }

        private fun MutableMap<String, String?>.getData(key: String): String? {
            val value = this.getOrElse(key) { null }
            this.remove(key)
            return value
        }

        private fun getUriType(data: MutableMap<String, String?>): String {
            if (data.isNullOrEmpty()) return ""
            if (data.containsKey(KEY_DEEP_LINK)) return KEY_DEEP_LINK
            if (data.containsKey(KEY_URL)) return KEY_URL
            if (data.containsKey(KEY_APP_PACKAGE)) return KEY_APP_PACKAGE
            if (data.containsKey(KEY_INBOX)) return KEY_INBOX
            if (data.containsKey(KEY_URL_INTERNAL)) return KEY_URL_INTERNAL
            return if (data.containsKey(KEY_APP_DESTROY_PUSH)) KEY_APP_DESTROY_PUSH else ""
        }

        private fun getPushOpenUriString(data: MutableMap<String, String?>): String? {
            var url: String? = null
            var key: String? = null
            if (data.containsKey(KEY_DEEP_LINK)) {
                url = data[KEY_DEEP_LINK]
                key = KEY_DEEP_LINK
            } else if (data.containsKey(KEY_APP_PACKAGE)) {
                url = data[KEY_APP_PACKAGE]
                key = KEY_APP_PACKAGE
            } else if (data.containsKey(KEY_INBOX)) {
                url = data[KEY_INBOX]
                key = KEY_INBOX
            } else {
                if (data.containsKey(KEY_URL)) {
                    url = data[KEY_URL]
                    key = KEY_URL
                } else if (data.containsKey(KEY_URL_INTERNAL)) {
                    url = data[KEY_URL_INTERNAL]
                    key = KEY_URL_INTERNAL
                }
                if (url != null && !url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://$url"
                }
            }
            key?.let { data.remove(it) }
            return url ?: ""
        }
    }
}
