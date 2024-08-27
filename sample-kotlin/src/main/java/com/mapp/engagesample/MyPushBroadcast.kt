package com.mapp.engagesample

import android.util.Log
import com.appoxee.shared.LocalPushBroadcast
import com.appoxee.shared.MappPush

class MyPushBroadcast : LocalPushBroadcast() {
    private fun log(action: String, push: MappPush) {
        val title = push.title
        val content = push.content
        Log.i("MyPushBroadcast", "ACTION: $action; PUSH: $push; TITLE: $title; CONTENT: $content")
    }

    override fun onReceived(push: MappPush) {
        log("onReceived", push)
    }

    override fun onOpened(push: MappPush) {
        log("onOpened", push)
    }

    override fun onSilent(push: MappPush) {
        log("onReceived", push)
    }

    override fun onDismissed(push: MappPush) {
        log("onDismissed", push)
    }

    override fun onButtonClick(
        push: MappPush
    ) {
        log("onButtonClick", push)
    }

    override fun onRichPush(push: MappPush) {
        log("onRichPush", push)
    }
}