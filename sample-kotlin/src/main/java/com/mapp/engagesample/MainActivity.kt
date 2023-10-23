package com.mapp.engagesample

import android.os.Bundle
import android.util.Log
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.appoxee.Appoxee
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.MappCallback
import com.appoxee.shared.MappResult
import com.appoxee.internal.model.response.DevicePayload

class MainActivity : ComponentActivity(), AppoxeeObserver {

    private val TAG = MainActivity::class.java.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Appoxee.instance().subscribe(this)
    }

    override fun onReadyStatusChanged(status: Boolean) {
        findViewById<Switch>(R.id.switchReady).apply {
            isEnabled = false
            isChecked = status
        }

        if (status) {
            Log.w(TAG, "Appoxee SDK is Ready!!!")
            Appoxee.instance().getDevice(callback = object : MappCallback<DevicePayload> {
                override fun onResult(mappResult: MappResult<DevicePayload>) {
                    if (mappResult.isSuccess()) {
                        Log.d(TAG, "SUCCESS IN MAIN ACTIVITY: ${mappResult.getData()}")
                        findViewById<TextView>(R.id.tvDevice).text = mappResult.getData().toString()
                    } else {
                        Log.e(TAG, "ERROR IN MAIN ACTIVITY: ${mappResult.getError()}")
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        Appoxee.instance().unsubscribe(this)
        super.onDestroy()
    }
}