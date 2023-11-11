package com.appoxee.internal.model.response

import com.appoxee.internal.util.arrayToList
import com.appoxee.internal.util.getLongOrDefault
import com.appoxee.internal.util.getStringOrEmpty
import com.appoxee.internal.util.toMap
import org.json.JSONObject

data class AppConfigPayload constructor(
    val id: Long,
    val mailboxTitle: String,
    val rtl: String,
    val moreApps: String,
    val feedback: String,
    val isDev: String,
    val inbox: String,
    val coppa: String,
    val customInbox: String,
    val apiLink: String,
    val sqsLink: String,
    val hasIntegration: String,
    val googlePid: String,
    val displayLast: String,
    val hasFakeAlias: String,
    val categories: List<Category>,
) {
    companion object {
        fun fromJson(json: JSONObject): AppConfigPayload {
            return AppConfigPayload(
                id = json.getLong("id"),
                mailboxTitle = json.getStringOrEmpty("mailboxTitle"),
                rtl = json.getStringOrEmpty("RTL"),
                moreApps = json.getStringOrEmpty("moreApps"),
                feedback = json.getStringOrEmpty("feedback"),
                isDev = json.getStringOrEmpty("is_dev"),
                inbox = json.getStringOrEmpty("inbox"),
                coppa = json.getStringOrEmpty("inbox"),
                customInbox = json.getStringOrEmpty("customInbox"),
                apiLink = json.getStringOrEmpty("api_link"),
                sqsLink = json.getStringOrEmpty("sqs_link"),
                hasIntegration = json.getStringOrEmpty("has_integration"),
                googlePid = json.getStringOrEmpty("google_pid"),
                displayLast = json.getStringOrEmpty("display_last"),
                hasFakeAlias = json.getStringOrEmpty("has_fake_alias"),
                categories = json.arrayToList("categories") { Category.fromJson(it) },
            )
        }
    }
}

data class Category(
    val buttons: List<Button>,
    val categoryId: Long,
    val isContextMinimal: Boolean,
    val isCustomCategory: Boolean,
    val name: String,
    val title: String?,
    val type: Long,
) {
    companion object {
        fun fromJson(json: JSONObject): Category {
            return Category(
                buttons = json.arrayToList("buttons") { Button.fromJson(it) },
                categoryId = json.getLongOrDefault("categoryId", 0),
                isContextMinimal = json.getBoolean("isContextMinimal"),
                isCustomCategory = json.getBoolean("isCustomCategory"),
                name = json.getStringOrEmpty("name"),
                title = json.getStringOrEmpty("title"),
                type = json.getLongOrDefault("type", 0)
            )
        }
    }
}

data class Button(
    val bgActionMandatory: Boolean,
    val id: Long,
    val index: Long,
    val isAuthNeeded: Boolean,
    val isDestructive: Boolean,
    val isForeground: Boolean,
    val title: String,
    val localizedTitle: Map<String, String>,
) {
    companion object {
        fun fromJson(json: JSONObject): Button {
            return Button(
                bgActionMandatory = json.getBoolean("bgActionMandatory"),
                id = json.getLongOrDefault("id", 0),
                index = json.getLongOrDefault("index", 0),
                isAuthNeeded = json.getBoolean("isAuthNeeded"),
                isDestructive = json.getBoolean("isDestructive"),
                isForeground = json.getBoolean("isForeground"),
                title = json.getStringOrEmpty("title"),
                localizedTitle = json.getJSONObject("localizedTitle").toMap<String>(excludeNulls = true).mapValues { it.value!! }
            )
        }
    }
}