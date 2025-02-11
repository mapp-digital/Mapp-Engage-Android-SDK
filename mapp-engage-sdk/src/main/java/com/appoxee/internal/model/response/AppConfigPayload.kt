package com.appoxee.internal.model.response

import android.os.Parcelable
import com.appoxee.internal.ui.push.model.CategoryType
import com.appoxee.internal.util.Logger
import com.appoxee.internal.util.arrayToList
import com.appoxee.internal.util.getLongOrDefault
import com.appoxee.internal.util.getStringOrEmpty
import com.appoxee.internal.util.toMap
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
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

    fun toJSON(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("mailboxTitle", mailboxTitle)
            put("RTL", rtl)
            put("moreApps", moreApps)
            put("feedback", feedback)
            put("is_dev", isDev)
            put("inbox", inbox)
            put("coppa", coppa)
            put("customInbox", customInbox)
            put("api_link", apiLink)
            put("sqs_link", sqsLink)
            put("has_integration", hasIntegration)
            put("google_pid", googlePid)
            put("display_last", displayLast)
            put("has_fake_alias", hasFakeAlias)
            put("categories", JSONArray().apply {
                categories.forEach { put(it.toJSON()) }
            })
        }
    }

    companion object {
        fun fromJson(json: JSONObject): AppConfigPayload {
            try {
                val appConfig = AppConfigPayload(
                    id = json.getLong("id"),
                    mailboxTitle = json.getStringOrEmpty("mailboxTitle"),
                    rtl = json.getStringOrEmpty("RTL"),
                    moreApps = json.getStringOrEmpty("moreApps"),
                    feedback = json.getStringOrEmpty("feedback"),
                    isDev = json.getStringOrEmpty("is_dev"),
                    inbox = json.getStringOrEmpty("inbox"),
                    coppa = json.getStringOrEmpty("coppa"),
                    customInbox = json.getStringOrEmpty("customInbox"),
                    apiLink = json.getStringOrEmpty("api_link"),
                    sqsLink = json.getStringOrEmpty("sqs_link"),
                    hasIntegration = json.getStringOrEmpty("has_integration"),
                    googlePid = json.getStringOrEmpty("google_pid"),
                    displayLast = json.getStringOrEmpty("display_last"),
                    hasFakeAlias = json.getStringOrEmpty("has_fake_alias"),
                    categories = json.arrayToList("categories") { Category.fromJson(it) },
                )

                return appConfig
            } catch (e: Exception) {
                Logger.e(AppConfigPayload::class.java.name, e.message ?: "", e)
                throw e
            }
        }
    }
}

@Parcelize
data class Category(
    val buttons: List<Button> = emptyList(),
    val categoryId: Long = 0,
    val isContextMinimal: Boolean = false,
    val isCustomCategory: Boolean = false,
    val name: CategoryType?,
    val title: String? = null,
    val type: Long = 0,
) : Parcelable {
    fun toJSON(): JSONObject {
        return JSONObject().apply {
            put("categoryId", categoryId)
            put("isContextMinimal", isContextMinimal)
            put("isCustomCategory", isCustomCategory)
            put("name", name?.value)
            put("title", title)
            put("type", type)
            put("buttons", JSONArray().apply {
                buttons.forEach {
                    put(it.toJSON())
                }
            })
        }
    }

    override fun toString(): String {
        return super.toString()
    }

    companion object {
        fun fromJson(json: JSONObject): Category {
            val categoryName = json.getStringOrEmpty("name")
            return Category(
                buttons = json.arrayToList("buttons") { Button.fromJson(it) },
                categoryId = json.getLongOrDefault("categoryId", 0),
                isContextMinimal = json.getBoolean("isContextMinimal"),
                isCustomCategory = json.getBoolean("isCustomCategory"),
                name = CategoryType.fromString(categoryName),
                title = json.getStringOrEmpty("title"),
                type = json.getLongOrDefault("type", 0)
            )
        }
    }
}

@Parcelize
data class Button(
    val index: Long = 1,
    val title: String,
    val isForeground: Boolean = false,
    val isDestructive: Boolean = false,
    val isAuthNeeded: Boolean = false,
    val bgActionMandatory: Boolean = false,
    val id: Long = 0,
    val localizedTitle: Map<String, String> = emptyMap(),
) : Parcelable {
    fun toJSON(): JSONObject {
        return JSONObject().apply {
            put("bgActionMandatory", bgActionMandatory)
            put("id", id)
            put("index", index)
            put("isAuthNeeded", isAuthNeeded)
            put("isDestructive", isDestructive)
            put("isForeground", isForeground)
            put("title", title)
            put("localizedTitle", JSONObject().apply {
                localizedTitle.keys.forEach {
                    put(it, localizedTitle.getValue(it))
                }
            })
        }
    }

    fun getLocalizedTitle(lang: String?): String {
        if (lang.isNullOrEmpty() || !localizedTitle.containsKey(lang)) return title
        return try {
            localizedTitle.getValue(lang)
        } catch (e: Exception) {
            title
        }
    }

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
                localizedTitle = json.getJSONObject("localizedTitle")
                    .toMap<String>(excludeNulls = true).mapValues { it.value!! }
            )
        }
    }
}