package com.appoxee.internal.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.appoxee.internal.model.request.events.PushAction
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.ui.custom.MediaDialog
import com.appoxee.internal.util.CompatExt.getParcelableCompat
import com.appoxee.internal.util.LibExt.startMainActivity
import com.appoxee.internal.util.Logger
import java.util.Objects

internal class ActivityLifecycleHandler(context: Context) : Application.ActivityLifecycleCallbacks {

    private val TAG = ActivityLifecycleHandler::class.java.name

    private val launchingIntent =
        context.packageManager.getLaunchIntentForPackage(context.packageName)

    private var launchingActivity: Activity? = null

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
        setLaunchingActivity(activity)
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity.componentName == launchingIntent?.component) {
            launchingActivity = null
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    private fun setLaunchingActivity(activity: Activity) {
        val launchingClassName = launchingIntent?.component?.className
        Logger.d(TAG, "LAUNCHING CLASS NAME: $launchingClassName")
        if (activity.componentName == launchingIntent?.component) {
            launchingActivity = activity
            val pushData = activity.intent?.extras?.getParcelableCompat<PushData>("pushData")
            val action = PushAction.fromString(activity.intent?.action) ?: return

            if (Objects.equals(action, PushAction.OPEN_RICH_PUSH)) {
                pushData?.let {
                    handleRichPush(activity, it)
                }
            }
        }
    }

    fun handleRichPush(context: Context, pushData: PushData) {
        if (launchingActivity != null) {
            (launchingActivity as FragmentActivity?)?.let { activity ->
                val dialog =
                    activity.supportFragmentManager.findFragmentByTag(MediaDialog::class.java.simpleName)

                if (dialog is DialogFragment) {
                    dialog.dismissAllowingStateLoss()
                }

                MediaDialog.getInstance(pushData).show(
                    activity.supportFragmentManager,
                    MediaDialog::class.java.simpleName
                )
            }

        } else {
            val bundle = Bundle().apply {
                putParcelable("pushData", pushData)
            }
            context.startMainActivity(bundle)
        }
    }
}