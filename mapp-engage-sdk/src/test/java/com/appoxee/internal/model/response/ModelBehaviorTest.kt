package com.appoxee.internal.model.response

import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.attributes.CustomAttributesPayload
import com.appoxee.internal.model.response.geo.Region
import com.appoxee.internal.model.response.inapp.ActionData
import com.appoxee.internal.model.response.inapp.BackgroundType
import com.appoxee.internal.model.response.inapp.ContentTemplates
import com.appoxee.internal.model.response.inapp.InappActionType
import com.appoxee.internal.model.response.inapp.InappButton
import com.appoxee.internal.model.response.inapp.InappType
import com.appoxee.internal.network.exceptions.UnknownNetworkException
import com.appoxee.internal.network.response.InappAdapter
import com.appoxee.internal.network.response.InboxAdapter
import com.appoxee.internal.network.response.StatusAdapter
import com.appoxee.internal.ui.push.model.CategoryType
import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

class ModelBehaviorTest {

    @Test
    fun app_config_round_trip_preserves_categories_and_buttons() {
        val original = AppConfigPayload(
            id = 7,
            mailboxTitle = "Inbox",
            rtl = "false",
            moreApps = "more",
            feedback = "feedback",
            isDev = "false",
            inbox = "true",
            coppa = "false",
            customInbox = "false",
            apiLink = "api",
            sqsLink = "sqs",
            hasIntegration = "true",
            googlePid = "pid",
            displayLast = "true",
            hasFakeAlias = "false",
            categories = listOf(
                Category(
                    buttons = listOf(
                        Button(
                            index = 1,
                            title = "Open",
                            isForeground = true,
                            localizedTitle = mapOf("de" to "Öffnen"),
                        )
                    ),
                    categoryId = 9,
                    categoryType = CategoryType.APX_YES_NO_OPEN,
                    title = "Category",
                    type = 22,
                )
            ),
        )

        val parsed = AppConfigPayload.fromJson(original.toJSON())

        assertThat(parsed).isEqualTo(original)
        assertThat(parsed.categories.single().buttons.single().getLocalizedTitle("de"))
            .isEqualTo("Öffnen")
        assertThat(parsed.categories.single().buttons.single().getLocalizedTitle("unknown"))
            .isEqualTo("Open")
    }

    @Test
    fun register_payload_uses_first_registration_that_is_not_the_user_id() {
        val payload = RegisterPayload.fromJSON(
            JSONObject()
                .put("dmcUserId", "user-id")
                .put("register", JSONArray(listOf("user-id", "alias", "other")))
        )

        assertThat(payload.dmcUserId).isEqualTo("user-id")
        assertThat(payload.alias).isEqualTo("alias")
        assertThat(payload.toJSON().getString("alias")).isEqualTo("alias")
    }

    @Test
    fun region_round_trip_preserves_all_fields() {
        val original = Region(
            id = 1,
            lat = 48.1,
            lng = 16.3,
            radius = 250,
            name = "Office",
            durationFrom = 10,
            durationTo = 20,
        )

        assertThat(Region.fromJSON(original.toJSON())).isEqualTo(original)
    }

    @Test
    fun custom_attributes_convert_between_map_and_json() {
        val payload = CustomAttributesPayload(mapOf("count" to 3, "active" to true))

        val json = payload.toJson()
        val parsed = CustomAttributesPayload.fromJson(JSONObject().put("get", json))

        assertThat(parsed["count"]).isEqualTo(3)
        assertThat(parsed["active"]).isEqualTo(true)
    }

    @Test
    fun action_data_maps_every_action_to_a_tracking_key() {
        val mappings = mapOf(
            InappActionType.DEEPLINK to TrackingKey.IA_MSG_DEEPLINK,
            InappActionType.APP_STORE to TrackingKey.IA_MSG_APP_STORE,
            InappActionType.DIALER to TrackingKey.IA_MSG_DIAL_NUMBER,
            InappActionType.CUSTOM to TrackingKey.IA_MSG_CUSTOM_ACTION,
        )

        mappings.forEach { (action, expected) ->
            assertThat(ActionData(null, false, action, messageId = 1).toTrackingKey())
                .isEqualTo(expected)
        }
        assertThat(
            ActionData(null, true, InappActionType.LANDING_PAGE, messageId = 1).toTrackingKey()
        ).isEqualTo(TrackingKey.IA_MSG_LANDING_PAGE_INTERNAL)
        assertThat(
            ActionData(null, false, InappActionType.LANDING_PAGE, messageId = 1).toTrackingKey()
        ).isEqualTo(TrackingKey.IA_MSG_LANDING_PAGE_EXTERNAL)
        assertThat(ActionData(null, false, null, messageId = 1).toTrackingKey())
            .isEqualTo(TrackingKey.IA_MSG_NOT_DISPLAYED)
    }

    @Test
    fun inapp_button_parses_action_and_exposes_action_data() {
        val button = InappButton.fromJSON(
            templateId = 12,
            json = JSONObject()
                .put("text", "Open")
                .put("text_color", "#ffffff")
                .put("background_color", "#000000")
                .put("action", "0")
                .put("link", "app://screen")
                .put("open_inApp", true),
        )

        assertThat(button.action).isEqualTo(InappActionType.DEEPLINK)
        assertThat(button.actionData.link).isEqualTo("app://screen")
        assertThat(button.actionData.messageId).isEqualTo(12)
    }

    @Test
    fun inapp_enums_map_known_and_fallback_values() {
        assertThat(BackgroundType.from(0)).isEqualTo(BackgroundType.HALF_BACKGROUND)
        assertThat(BackgroundType.from(100)).isEqualTo(BackgroundType.FULL_BACKGROUND)
        assertThat(InappType.from(1)).isEqualTo(InappType.BANNER)
        assertThat(InappType.from(2)).isEqualTo(InappType.DIALOG)
        assertThat(InappType.from(100)).isEqualTo(InappType.FULLSCREEN)
        assertThat(ContentTemplates.from("standard")).isEqualTo(ContentTemplates.STANDARD)
        assertThat(ContentTemplates.from("unknown")).isEqualTo(ContentTemplates.FULLSCREEN)
    }

    @Test
    fun response_adapters_handle_success_and_missing_data() {
        val statusSuccess = StatusAdapter().createResponse(200, null, null)
        val failure = IllegalStateException("failed")
        val statusFailure = StatusAdapter().createResponse(500, null, failure)
        val inboxSuccess = InboxAdapter("event-key").createResponse(
            200,
            JSONObject().put("event_id", "event-id").put("messages", JSONArray()),
            null,
        )
        val inboxFailure = InboxAdapter().createResponse(500, null, failure)
        val inappSuccess = InappAdapter().createResponse(
            200,
            JSONObject()
                .put("event_id", "event-id")
                .put("event_key", "event-key")
                .put("web_messages", JSONArray())
                .put("native_messages", JSONArray()),
            null,
        )
        val inappFailure = InappAdapter().createResponse(500, null, failure)

        assertThat(statusSuccess.isSuccess()).isTrue()
        assertThat(statusSuccess.data?.payload).isTrue()
        assertThat(statusFailure.error).isSameInstanceAs(failure)
        assertThat(inboxSuccess.data?.eventId).isEqualTo("event-id")
        assertThat(inboxFailure.error).isSameInstanceAs(failure)
        assertThat(inappSuccess.data?.eventKey).isEqualTo("event-key")
        assertThat(inappFailure.error).isSameInstanceAs(failure)
    }

    @Test
    fun unknown_network_exception_includes_message_and_cause() {
        val exception = UnknownNetworkException("network failed", IllegalStateException("cause"))

        assertThat(exception.toString()).contains("network failed")
        assertThat(exception.toString()).contains("cause")
    }
}
