package com.appoxee.internal.push.model

import android.os.Parcelable
import com.appoxee.internal.util.arrayToList
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

@Parcelize
internal data class PushButton(
    val fgActions: MutableList<FgAction> = mutableListOf(),
    val bgActions: MutableList<BgAction> = mutableListOf()
) :Parcelable{
    companion object {
        private const val FG_ACTION = "fgAction"
        private const val BG_ACTION = "bgAction"
        fun fromJSON(arr: JSONArray): List<PushButton> {
            val buttons = mutableListOf<PushButton>()
            for (i in 0 until arr.length()) {
                val json = arr.getJSONObject(i)
                json.let {
                    val pushButton = PushButton()
                    if (it.has(FG_ACTION) || it.has(BG_ACTION)) {
                        if (it.has(FG_ACTION)) {
                            val fgAction = FgAction.fromJSON(it.getJSONObject(FG_ACTION))
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