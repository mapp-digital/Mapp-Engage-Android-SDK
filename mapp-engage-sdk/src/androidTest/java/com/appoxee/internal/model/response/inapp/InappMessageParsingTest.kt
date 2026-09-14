package com.appoxee.internal.model.response.inapp

import android.util.Base64
import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

class InappMessageParsingTest {

    @Test
    fun nativeMessageMapsPayloadFields() {
        val button = JSONObject()
            .put("text", "Open")
            .put("text_color", "#ffffff")
            .put("background_color", "#000000")
            .put("action", "0")
            .put("link", "mapp://message")
            .put("open_inApp", true)
        val payload = JSONObject()
            .put("template_id", 42)
            .put("content", "message body")
            .put("type", 1)
            .put("behaviour", JSONObject().put("delay_seconds", 2).put("display_seconds", 10))
            .put("location", JSONObject().put("position", 1).put("height", 25).put("width", 75))
            .put("imageURL", "https://example.com/image.png")
            .put("title", "Title")
            .put("title_color", "#111111")
            .put("template_background_color", "#222222")
            .put("content_color", "#333333")
            .put("buttons", JSONArray().put(button))
            .put("content_template_id", "standard")

        val message = NativeInappMessage.fromJSON(payload, "event-id", "event-key")

        assertThat(message.originalEventId).isEqualTo("event-id")
        assertThat(message.originalEventKey).isEqualTo("event-key")
        assertThat(message.templateId).isEqualTo(42)
        assertThat(message.content).isEqualTo("message body")
        assertThat(message.type).isEqualTo(InappType.BANNER)
        assertThat(message.behaviour).isEqualTo(Behaviour(2, 10))
        assertThat(message.location).isEqualTo(Location(BannerPosition.BOTTOM, 25, 75))
        assertThat(message.imageUrl).isEqualTo("https://example.com/image.png")
        assertThat(message.title).isEqualTo("Title")
        assertThat(message.titleColor).isEqualTo("#111111")
        assertThat(message.templateBackgroundColor).isEqualTo("#222222")
        assertThat(message.contentColor).isEqualTo("#333333")
        assertThat(message.contentTemplateId).isEqualTo(ContentTemplates.STANDARD)
        assertThat(message.buttons).containsExactly(
            InappButton(
                text = "Open",
                textColor = "#ffffff",
                backgroundColor = "#000000",
                action = InappActionType.DEEPLINK,
                link = "mapp://message",
                openInApp = true,
                templateId = 42
            )
        )
    }

    @Test
    fun webMessageDecodesContentAndMapsOptionalObjects() {
        val encodedContent = Base64.encodeToString("<p>Hello</p>".toByteArray(), Base64.NO_WRAP)
        val payload = JSONObject()
            .put("template_id", 7)
            .put("content", encodedContent)
            .put("type", 2)
            .put("behaviour", JSONObject().put("delay_seconds", 1).put("display_seconds", 5))
            .put("location", JSONObject().put("position", 0).put("height", 40).put("width", 80))

        val message = WebInappMessage.fromJSON(payload, "event-id", "event-key")

        assertThat(message.originalEventId).isEqualTo("event-id")
        assertThat(message.originalEventKey).isEqualTo("event-key")
        assertThat(message.templateId).isEqualTo(7)
        assertThat(message.content).isEqualTo("<p>Hello</p>")
        assertThat(message.type).isEqualTo(InappType.DIALOG)
        assertThat(message.behaviour).isEqualTo(Behaviour(1, 5))
        assertThat(message.location).isEqualTo(Location(BannerPosition.TOP, 40, 80))
    }
}
