package com.appoxee.internal.ui.inapp

interface Template {

    val TAG
        get() = this::class.java.name

    val buttonRadius: Int
        get() = 15
    val dialogRadius: Int
        get() = 20

    fun show()
}