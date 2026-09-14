package com.appoxee.internal.model.request

import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.model.request.events.InappEvent
import com.appoxee.internal.model.request.events.MessageContext
import com.appoxee.internal.model.request.events.PushEvent
import com.appoxee.internal.model.request.events.Tracking
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.request.geo.GeoEvent
import com.appoxee.internal.model.request.geo.RegionStatus
import com.appoxee.internal.model.response.DevicePayload
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

class RequestSerializationTest {

    @Test
    fun activation_serializes_time_spent() {
        val activation = Activation(42).asJson().getJSONObject("activation")

        assertThat(activation.getString("timeSpent")).isEqualTo("42")
    }

    @Test
    fun get_requests_serialize_their_expected_envelopes() {
        val attributes = GetAttributes(listOf("first", "second")).asJson().getJSONArray("get")

        assertThat(attributes.getString(0)).isEqualTo("first")
        assertThat(attributes.getString(1)).isEqualTo("second")
        assertThat(GetAppConfig().asJson().has("app_conf")).isTrue()
    }

    @Test
    fun message_body_serializes_all_fields() {
        val json = MessageBody(
            timestamp = 123,
            id = "message-id",
            userId = "user-id",
            alias = "alias",
            eventKey = "event-key",
            deviceId = "device-id",
        ).asJson()

        assertThat(json.getLong("timestamp")).isEqualTo(123)
        assertThat(json.getString("id")).isEqualTo("message-id")
        assertThat(json.getString("user_id")).isEqualTo("user-id")
        assertThat(json.getString("alias")).isEqualTo("alias")
        assertThat(json.getString("event_key")).isEqualTo("event-key")
        assertThat(json.getString("device_id")).isEqualTo("device-id")
    }

    @Test
    fun opt_in_and_opt_out_move_the_push_token_between_fields() {
        val optIn = OptIn("current-token").asJson().getJSONObject("set")
        val optOut = OptOut("backup-token").asJson().getJSONObject("set")

        assertThat(optIn.getString("pushToken")).isEqualTo("current-token")
        assertThat(optIn.getString("pushToken_bk")).isEmpty()
        assertThat(optOut.getString("pushToken")).isEmpty()
        assertThat(optOut.getString("pushToken_bk")).isEqualTo("backup-token")
    }

    @Test
    fun set_alias_serializes_alias() {
        val json = SetAlias("customer-alias").asJson().getJSONObject("set")

        assertThat(json.getString("alias")).isEqualTo("customer-alias")
    }

    @Test
    fun set_attributes_serializes_supported_types_and_as_string_is_safe_first() {
        val request = SetAttributes(
            mapOf(
                "number" to 7,
                "boolean" to true,
                "text" to "value",
                "unsupported" to listOf("value"),
            )
        )

        val json = JSONObject(request.asString()).getJSONObject("set")

        assertThat(json.getInt("number")).isEqualTo(7)
        assertThat(json.getBoolean("boolean")).isTrue()
        assertThat(json.getString("text")).isEqualTo("value")
        assertThat(json.has("unsupported")).isFalse()
    }

    @Test
    fun tags_serializes_set_and_remove_actions_and_as_string_is_safe_first() {
        val set = JSONObject(Tags(listOf("one", "two"), TagsAction.SET).asString())
            .getJSONObject("tags")
            .getJSONArray("set")
        val remove = JSONObject(Tags(listOf("old"), TagsAction.REMOVE).asString())
            .getJSONObject("tags")
            .getJSONArray("remove")

        assertThat(set.getString(0)).isEqualTo("one")
        assertThat(set.getString(1)).isEqualTo("two")
        assertThat(remove.getString(0)).isEqualTo("old")
    }

    @Test
    fun request_body_adds_metadata_and_optional_alias() {
        val json = RequestBody(
            key = "request-key",
            actions = SetAlias("customer-alias"),
            alias = "request-alias",
        ).asJson()

        assertThat(json.getString("key")).isEqualTo("request-key")
        assertThat(json.getString("alias")).isEqualTo("request-alias")
        assertThat(json.getJSONObject("actions").has("time")).isTrue()
        assertThat(json.getJSONObject("actions").getString("requestId")).isNotEmpty()
    }

    @Test
    fun push_event_serializes_supported_click_type_and_omits_unsupported_one() {
        val supported = PushEvent(
            tenantId = "tenant",
            eventType = EventType.BUTTON1,
            messageId = 10,
            dmcUserId = "user",
            sendoutId = 20,
            clickType = ClickType.OPEN_STORE,
        ).asJson()
        val unsupported = PushEvent(
            tenantId = "tenant",
            eventType = EventType.DISMISS,
            messageId = 10,
            dmcUserId = "user",
            sendoutId = 20,
            clickType = ClickType.DISMISS,
        ).asJson()

        assertThat(supported.getInt("click_action_type")).isEqualTo(ClickType.OPEN_STORE.ordinal)
        assertThat(unsupported.has("click_action_type")).isFalse()
    }

    @Test
    fun inapp_event_components_can_serialize_before_as_json_is_called() {
        val messageContext = MessageContext("event-id", 99)
        val tracking = Tracking(TrackingKey.IA_MSG_DISPLAYED, mapOf("source" to "inbox"))
        val event = InappEvent(
            device = DevicePayload(
                dmcUserId = "user-id",
                udidHashed = "device-id",
                pushToken = "push-token",
                alias = "alias",
            ),
            messageContext = messageContext,
            tracking = tracking,
        )

        val contextJson = JSONObject(messageContext.asString())
        val trackingJson = JSONObject(tracking.asString())
        val eventJson = JSONObject(event.asString())

        assertThat(contextJson.getString("original_event_id")).isEqualTo("event-id")
        assertThat(contextJson.getLong("template_id")).isEqualTo(99)
        assertThat(trackingJson.getString("tracking_key")).isEqualTo("ia_message_displayed")
        assertThat(trackingJson.getJSONObject("tracking_attributes").getString("source"))
            .isEqualTo("inbox")
        assertThat(eventJson.getString("device_id")).isEqualTo("device-id")
        assertThat(eventJson.getBoolean("push_enabled")).isTrue()
    }

    @Test
    fun region_status_serializes_timestamp_and_location_data() {
        val json = RegionStatus(
            timestamp = 123,
            geoEvent = GeoEvent.EXIT,
            dmcUserId = "user-id",
            latitude = 48.1,
            longitude = 16.3,
            regionId = 42,
            timeZone = "Europe/Budapest",
            version = 3,
            applicationId = "application-id",
        ).asJson().getJSONObject("region_status")

        assertThat(json.getLong("timeStamp")).isEqualTo(123)
        assertThat(json.getInt("event_type")).isEqualTo(GeoEvent.EXIT.ordinal)
        assertThat(json.getString("dmc_user_id")).isEqualTo("user-id")
        assertThat(json.getDouble("latitude")).isEqualTo(48.1)
        assertThat(json.getDouble("longitude")).isEqualTo(16.3)
        assertThat(json.getLong("region_id")).isEqualTo(42)
        assertThat(json.getString("time_zone")).isEqualTo("Europe/Budapest")
        assertThat(json.getInt("version")).isEqualTo(3)
        assertThat(json.getString("application_id")).isEqualTo("application-id")
    }

    @Test
    fun get_regions_serializes_query_parameters() {
        val json = com.appoxee.internal.model.request.geo.GetRegions(
            latitude = 48.1,
            longitude = 16.3,
            version = 4,
            applicationId = 99,
            pageSize = 50,
        ).asJson().getJSONObject("get_regions")

        assertThat(json.getDouble("latitude")).isEqualTo(48.1)
        assertThat(json.getDouble("longitude")).isEqualTo(16.3)
        assertThat(json.getInt("version")).isEqualTo(4)
        assertThat(json.getLong("application_id")).isEqualTo(99)
        assertThat(json.getInt("page_size")).isEqualTo(50)
    }
}
