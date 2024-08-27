package com.appoxee.shared

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ActionButton(val uri: Uri, val action: String?) : Parcelable {
}