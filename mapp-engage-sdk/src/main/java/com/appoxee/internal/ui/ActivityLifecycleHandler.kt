package com.appoxee.internal.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.ui.custom.MediaDialog
import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.util.CompatExt.getParcelableCompat
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.LibraryExtensions.startMainActivity
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Objects
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

internal class ActivityLifecycleHandler(
    context: Context,
    private val statsClient: StatsClient,
    private val scope: CoroutineScope,
    private val dispatchersProvider: DispatchersProvider,
) : Application.ActivityLifecycleCallbacks {

    private val TAG = ActivityLifecycleHandler::class.java.name

    private val launchingIntent =
        context.packageManager.getLaunchIntentForPackage(context.packageName)

    private var launchingActivity: Activity? = null

    private var startingTimestamp = AtomicLong()

    private val visibleActivities: MutableList<KClass<out Activity>> = mutableListOf()

    private var isApplicationInForeground: AtomicBoolean = AtomicBoolean(false)

    init {
        scope.launch(dispatchersProvider.mainDispatcher) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    isApplicationInForeground.set(event <= Lifecycle.Event.ON_RESUME)
                }
            })
        }
    }

    fun isInForeground(): Boolean {
        return isApplicationInForeground.get()
    }

    fun isVisible(activity: Activity): Boolean {
        return visibleActivities.contains(activity::class)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.d(TAG, "CREATED ACTIVITY: ${activity::class.java.name}")
    }

    override fun onActivityStarted(activity: Activity) {
        Logger.d(TAG, "STARTED ACTIVITY: ${activity::class.java.name}")
        setLaunchingActivity(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        visibleActivities.add(activity::class)
    }

    override fun onActivityPaused(activity: Activity) {
        visibleActivities.remove(activity::class)
    }

    override fun onActivityStopped(activity: Activity) {
        Logger.d(TAG, "STOPPED ACTIVITY: ${activity::class.java.name}")
        clearLaunchingActivity(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        Logger.d(TAG, "DESTROYED ACTIVITY: ${activity::class.java.name}")
    }

    private fun clearLaunchingActivity(activity: Activity) {
        if (activity.componentName == launchingIntent?.component) {
            scope.launch {
                val activeTimeSeconds =
                    ((System.currentTimeMillis() - startingTimestamp.get()) / 1000).toInt()
                statsClient.reportActivation(activeTimeSeconds)
            }
            launchingActivity = null
        }
    }

    private fun setLaunchingActivity(activity: Activity) {
        val launchingClassName = launchingIntent?.component?.className
        if (activity.componentName == launchingIntent?.component) {
            Logger.d(TAG, "LAUNCHING CLASS NAME: $launchingClassName")
            startingTimestamp.set(System.currentTimeMillis())
            launchingActivity = activity
            activity.intent?.extras?.getParcelableCompat<PushData>("pushData")?.let { pushData ->
                ClickType.fromString(activity.intent.action).let { action ->
                    if (Objects.equals(action, ClickType.OPEN_RICH_PUSH)) {
                        handleRichPush(activity, pushData)
                    }
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
            context.startMainActivity(pushData)
        }
    }
}