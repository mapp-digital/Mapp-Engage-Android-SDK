package com.mapp.engagesample

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.appoxee.Appoxee

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val isMappSdkReady = Appoxee.instance().isReady()

        if (isMappSdkReady) {
            Appoxee.instance().setAlias("")
        }
    }
}