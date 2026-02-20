package com.appoxee.shared

import android.net.Uri
import android.os.Parcelable
import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.ui.push.model.PushUriType.Companion.toPushAction
import kotlinx.parcelize.Parcelize
import androidx.core.net.toUri

@ConsistentCopyVisibility
@Parcelize
data class MappPush internal constructor(
    val id: String?,
    val title: String?,
    val content: String?,
    val actionUri: Uri?,
    val type: String?,
    val userId: String?,
    val customerId: String?,
    val category: String?,
    val language: String?,
    val actionButtons: List<ActionButton>,
    val silentType: String? = null,
    val silentData: String? = null,
    val contentAvailable: Boolean = false,
    val extraFields:Map<String, String> = emptyMap(),
) : Parcelable {
    internal constructor(
        pushData: PushData,
    ) : this(
        id = pushData.id.toString(),
        title = pushData.title,
        content = pushData.bigText,
        actionUri = pushData.actionUri,
        type = pushData.type,
        userId = pushData.userId,
        customerId = pushData.customerId,
        category = pushData.category?.title,
        language = pushData.language,
        silentType = pushData.silentType,
        silentData = pushData.silentData,
        contentAvailable = pushData.contentAvailable,
        actionButtons = pushData.buttonList.map { buttonList ->
            val data = mutableListOf<ActionButton>()
            buttonList?.fgActions?.forEach {
                data.add(
                    ActionButton(
                        uri = it.getAction().toUri(),
                        action = it.getUriType().toPushAction().value
                    )
                )
            }
            data
        }.flatten(),
        extraFields = pushData.extraFields,
    )
}
