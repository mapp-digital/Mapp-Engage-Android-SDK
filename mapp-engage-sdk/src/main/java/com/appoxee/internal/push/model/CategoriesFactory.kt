package com.appoxee.internal.push.model

import com.appoxee.internal.model.response.Button
import com.appoxee.internal.model.response.Category
import com.appoxee.internal.storage.Storage

internal class CategoriesFactory(private val storage: Storage) {
    private val defaultCategories = listOf(
        Category(
            name = CategoryType.APX_YES_NO_OPEN,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(
                    index = 1,
                    title = "Yes",
                    isForeground = true,
                    isDestructive = false,
                    isAuthNeeded = false
                ),
                Button(
                    index = 2,
                    title = "No",
                    isForeground = false,
                    isDestructive = true,
                    isAuthNeeded = false
                )
            )
        ),
        Category(
            name = CategoryType.APX_YES_NO_DISMISS,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(
                    index = 1,
                    title = "Yes",
                    isForeground = false,
                    isDestructive = true,
                    isAuthNeeded = false
                ),
                Button(
                    index = 2,
                    title = "No",
                    isForeground = false,
                    isDestructive = true,
                    isAuthNeeded = false
                )
            )
        ),
        Category(
            name = CategoryType.APX_ACC_DEC_OPEN,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(
                    index = 1,
                    title = "Accept",
                    isForeground = true,
                    isDestructive = false,
                    isAuthNeeded = false
                ),
                Button(
                    index = 2,
                    title = "Decline",
                    isForeground = false,
                    isDestructive = true,
                    isAuthNeeded = false
                )
            )
        ),
        Category(
            name = CategoryType.APX_ACC_DEC_DISMISS,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(
                    index = 1,
                    title = "Accept",
                    isForeground = false,
                    isDestructive = true,
                    isAuthNeeded = false
                ),
                Button(
                    index = 2,
                    title = "Dismiss",
                    isForeground = false,
                    isDestructive = true,
                    isAuthNeeded = false
                )
            )
        ),
        Category(
            name = CategoryType.APX_PLAY_OPEN,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(
                    index = 1,
                    title = "Play Now",
                    isForeground = true,
                    isDestructive = false,
                    isAuthNeeded = false
                ),
            )
        ),
        Category(
            name = CategoryType.APX_BUY_OPEN,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(
                    index = 1,
                    title = "Buy Now",
                    isForeground = true,
                    isDestructive = false,
                    isAuthNeeded = false
                ),
            )
        ),
        Category(
            name = CategoryType.APX_FOLLOW_DISMISS,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(
                    index = 1,
                    title = "Follow",
                    isForeground = false,
                    isDestructive = true,
                    isAuthNeeded = false
                )
            )
        ),
        Category(
            name = CategoryType.APX_REMIND_ME_DISMISS,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(
                    index = 1,
                    title = "Remind Me Later",
                    isForeground = false,
                    isDestructive = true,
                    isAuthNeeded = false
                )
            )
        ),
        Category(
            name = CategoryType.APX_REDEEM_OPEN,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(
                    index = 1,
                    title = "Redeem",
                    isForeground = true,
                    isDestructive = false,
                    isAuthNeeded = false
                ),
                Button(
                    index = 2,
                    title = "No Thanks",
                    isForeground = false,
                    isDestructive = true,
                    isAuthNeeded = false
                )
            )
        ),
        Category(
            name = CategoryType.APX_READ_OPEN,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(
                    index = 1,
                    title = "Read More",
                    isForeground = true,
                    isDestructive = false,
                    isAuthNeeded = false
                ),
            )
        ),
        Category(
            name = CategoryType.APX_ACCEPT_NOTIFICATION_SETTINGS_OPEN,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(
                    index = 1,
                    title = "Accept",
                    isForeground = true,
                    isDestructive = false,
                    isAuthNeeded = false
                ),
                Button(
                    index = 2,
                    title = "Notif. Settings",
                    isForeground = false,
                    isDestructive = true,
                    isAuthNeeded = false
                )
            )
        ),
        Category(
            name = CategoryType.APX_YES_NOTIFICATION_SETTINGS_OPEN,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(
                    index = 1,
                    title = "Yes",
                    isForeground = true,
                    isDestructive = false,
                    isAuthNeeded = false
                ),
                Button(
                    index = 2,
                    title = "Notif. Settings",
                    isForeground = false,
                    isDestructive = true,
                    isAuthNeeded = false
                )
            )
        ),
        Category(
            name = CategoryType.APX_CUSTOM_PUSH_1,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(
                    index = 1,
                    title = "Agree",
                    isForeground = true,
                    isDestructive = false,
                    isAuthNeeded = false
                ),
                Button(
                    index = 2,
                    title = "Disagree",
                    isForeground = false,
                    isDestructive = true,
                    isAuthNeeded = false
                )
            )
        ),
        Category(
            name = CategoryType.APX_FULL_CUSTOM_PUSH,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(
                    index = 1,
                    title = "Details Inside",
                    isForeground = true,
                    isDestructive = false,
                    isAuthNeeded = false
                ),
                Button(
                    index = 2,
                    title = "No Thanks",
                    isForeground = false,
                    isDestructive = true,
                    isAuthNeeded = false
                )
            )
        ),
        Category(
            name = CategoryType.APX_SHOP_REMIND_OPEN,
            isCustomCategory = false,
            isContextMinimal = false,
            buttons = listOf(
                Button(1, "Shop Now", true, false, false),
                Button(2, "Remind Me Later", false, true, false)
            )
        ),
    )

    internal suspend fun getCategories(): List<Category> {
        val appConfig=storage.getAppConfig()
        return appConfig?.categories ?: defaultCategories
    }
}