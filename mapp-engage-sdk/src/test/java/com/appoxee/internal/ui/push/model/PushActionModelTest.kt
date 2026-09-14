package com.appoxee.internal.ui.push.model

import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.response.Button
import com.appoxee.internal.model.response.Category
import com.appoxee.internal.ui.push.model.PushUriType.Companion.toPushAction
import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

class PushActionModelTest {

    @Test
    fun foreground_action_resolves_each_supported_uri_type_and_value() {
        val cases = listOf(
            FgAction(apxUrl = "https://example.com") to PushUriType.KEY_URL,
            FgAction(apxAid = "application-id") to PushUriType.KEY_APP_PACKAGE,
            FgAction(apxVc = "view-controller") to PushUriType.KEY_APX_VC,
            FgAction(apxInbox = "inbox") to PushUriType.KEY_INBOX,
            FgAction(apxUrlInternal = "https://example.com/internal") to PushUriType.KEY_URL_INTERNAL,
            FgAction(apxDpl = "app://screen") to PushUriType.KEY_DEEP_LINK,
            FgAction(apxDpl = "tel:123") to PushUriType.KEY_DIALER,
            FgAction() to PushUriType.KEY_LAUNCH_APP,
            FgAction(isDestructive = true) to PushUriType.KEY_APP_DESTROY_PUSH,
        )

        cases.forEach { (action, expectedType) ->
            assertThat(action.getUriType()).isEqualTo(expectedType)
            assertThat(action.getAction()).isEqualTo(
                when (expectedType) {
                    PushUriType.KEY_URL -> action.apxUrl
                    PushUriType.KEY_APP_PACKAGE -> action.apxAid
                    PushUriType.KEY_APX_VC -> action.apxVc
                    PushUriType.KEY_INBOX -> action.apxInbox
                    PushUriType.KEY_URL_INTERNAL -> action.apxUrlInternal
                    PushUriType.KEY_DEEP_LINK, PushUriType.KEY_DIALER -> action.apxDpl
                    PushUriType.KEY_LAUNCH_APP -> PushUriType.KEY_LAUNCH_APP.value
                    else -> PushUriType.KEY_APP_DESTROY_PUSH.value
                }
            )
        }
        assertThat(FgAction(isDestructive = true).isDestroyAction()).isTrue()
        assertThat(FgAction(apxUrl = "url", isDestructive = true).isDestroyAction()).isFalse()
    }

    @Test
    fun foreground_and_background_actions_parse_json() {
        val foreground = FgAction.fromJSON(JSONObject().put("apx_dpl", "app://screen"))
        val background = BgAction.fromJSON(
            JSONObject()
                .put("name", "name")
                .put("todo", "todo")
                .put("type", "type")
                .put("value", "value")
        )

        assertThat(foreground.getUriType()).isEqualTo(PushUriType.KEY_DEEP_LINK)
        assertThat(background).isEqualTo(BgAction("name", "todo", "type", "value"))
    }

    @Test
    fun push_buttons_parse_foreground_background_and_destructive_metadata() {
        val category = Category(
            buttons = listOf(
                Button(title = "Delete", isDestructive = true),
                Button(title = "Open"),
            ),
            categoryId = 1,
            categoryType = CategoryType.APX_CUSTOM_PUSH_1,
            type = 1,
        )
        val json = JSONArray()
            .put(JSONObject().put("fgAction", JSONObject().put("apx_url", "https://example.com")))
            .put(JSONObject().put("bgAction", JSONObject().put("name", "background")))
            .put(JSONObject())

        val buttons = PushButton.fromJSON(json, category)

        assertThat(buttons).hasSize(2)
        assertThat(buttons[0].fgActions.single().isDestructive).isTrue()
        assertThat(buttons[0].fgActions.single().getUriType()).isEqualTo(PushUriType.KEY_URL)
        assertThat(buttons[1].bgActions.single().name).isEqualTo("background")
    }

    @Test
    fun push_uri_types_map_to_click_types() {
        assertThat(PushUriType.KEY_URL.toPushAction()).isEqualTo(ClickType.OPEN_LANDING_PAGE)
        assertThat(PushUriType.KEY_APP_PACKAGE.toPushAction()).isEqualTo(ClickType.OPEN_STORE)
        assertThat(PushUriType.KEY_DEEP_LINK.toPushAction()).isEqualTo(ClickType.OPEN_DEEP_LINK)
        assertThat(PushUriType.KEY_DIALER.toPushAction()).isEqualTo(ClickType.OPEN_DIALER)
        assertThat(PushUriType.KEY_PLAY.toPushAction()).isEqualTo(ClickType.OPEN_RICH_PUSH)
        assertThat(PushUriType.KEY_LAUNCH_APP.toPushAction()).isEqualTo(ClickType.LAUNCH_APP)
        assertThat((null as PushUriType?).toPushAction()).isEqualTo(ClickType.DISMISS)
    }
}
