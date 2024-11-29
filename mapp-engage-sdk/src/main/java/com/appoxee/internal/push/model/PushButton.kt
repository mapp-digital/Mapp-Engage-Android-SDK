package com.appoxee.internal.push.model

import android.os.Parcelable
import com.appoxee.internal.model.response.Category
import kotlinx.parcelize.Parcelize
import org.json.JSONArray

@Parcelize
internal data class PushButton(
    val fgActions: MutableList<FgAction> = mutableListOf(),
    val bgActions: MutableList<BgAction> = mutableListOf()
) : Parcelable {
    companion object {
        private const val FG_ACTION = "fgAction"
        private const val BG_ACTION = "bgAction"

        fun fromJSON(arr: JSONArray, category: Category?): List<PushButton> {
            val buttons = mutableListOf<PushButton>()
            val categoryButtons = category?.buttons ?: emptyList()
            for (i in 0 until arr.length()) {
                val json = arr.getJSONObject(i)
                val catButton = categoryButtons.getOrElse(i) { null }
                val isDestructive = catButton?.isDestructive ?: false
                json.let {
                    val pushButton = PushButton()
                    if (it.has(FG_ACTION) || it.has(BG_ACTION)) {
                        if (it.has(FG_ACTION)) {
                            val fgAction = FgAction.fromJSON(
                                it.getJSONObject(FG_ACTION),
                                isDestructive = isDestructive
                            )
                            pushButton.fgActions.add(fgAction)
                        }
                        if (it.has(BG_ACTION)) {
                            val bgAction = BgAction.fromJSON(it.getJSONObject(BG_ACTION))
                            pushButton.bgActions.add(bgAction)
                        }
                        buttons.add(pushButton)
                    }
                }
            }

            return buttons
        }
    }
}