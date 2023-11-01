package com.mapp.engagesample

import android.os.Bundle
import android.widget.Switch
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.appoxee.Appoxee
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.MappCallback
import com.appoxee.shared.MappResult
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import eu.brrm.shared_ui.Util
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), AppoxeeObserver {

    private val TAG = MainActivity::class.java.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(eu.brrm.shared_ui.R.layout.activity_main)

        Appoxee.instance().subscribe(this)

        findViewById<MaterialButton>(eu.brrm.shared_ui.R.id.btnSetAlias).setOnClickListener {
            lifecycleScope.launch {
                val alias =
                    findViewById<TextInputEditText>(eu.brrm.shared_ui.R.id.editTextAlias).text.toString()
                if (alias.isNullOrBlank()) {
                    Util.showDialog(this@MainActivity, "Set Alias", "Alias can't be empty!")
                    return@launch
                }
                val result = Appoxee.instance().setAlias(alias).asSuspend()
                Util.showDialog(this@MainActivity, "DmcUserId", result.getData() ?: "")
            }
        }

        findViewById<MaterialButton>(eu.brrm.shared_ui.R.id.btnGetAlias).setOnClickListener {
            lifecycleScope.launch {
                val result = Appoxee.instance().getAlias().asSuspend()
                Util.showDialog(this@MainActivity, "Alias", result.getData() ?: "")
            }
        }

        findViewById<MaterialButton>(eu.brrm.shared_ui.R.id.btnGetDevice).setOnClickListener {
            lifecycleScope.launch {
                val result = Appoxee.instance().getDevice().asSuspend()
                Util.showDialog(this@MainActivity, "Device", result.getData().toString())
            }
        }

        findViewById<MaterialButton>(eu.brrm.shared_ui.R.id.btnFetchInboxMessages).setOnClickListener {
            lifecycleScope.launch {
                val result = Appoxee.instance().fetchInboxMessages("app_inbox").asSuspend()
                Util.showDialog(this@MainActivity, "Inbox Messages", result.getData().toString())
            }
        }

        findViewById<MaterialButton>(eu.brrm.shared_ui.R.id.btnFetchInappMessages).setOnClickListener {
//            lifecycleScope.launch {
//                val result = Appoxee.instance().fetchInappMessages("app_open").asSuspend()
//                Util.showDialog(this@MainActivity, "Inapp Messages", result.getData().toString())
//            }

            Appoxee.instance().fetchInappMessages("app_open").enqueue(object : MappCallback<InappResponse?>{
                override fun onResult(mappResult: MappResult<InappResponse?>) {
                    Util.showDialog(this@MainActivity, "Inapp Messages", mappResult.getData().toString())
                }
            })

        }

        findViewById<MaterialButton>(eu.brrm.shared_ui.R.id.btnTestCallExecute).setOnClickListener {
            lifecycleScope.launch {
                val result = Appoxee.instance().testCall().asSuspend()
                Util.showDialog(this@MainActivity, "Test Call (execute)", result.getData() ?: "")
            }
        }

        findViewById<MaterialButton>(eu.brrm.shared_ui.R.id.btnTestCallExecute).setOnClickListener {
            Appoxee.instance().testCall().enqueue(object : MappCallback<String> {
                override fun onResult(mappResult: MappResult<String>) {
                    Util.showDialog(
                        this@MainActivity,
                        "Test Call (enqueue)",
                        mappResult.getData() ?: ""
                    )
                }
            })
        }
    }

    override fun onDestroy() {
        Appoxee.instance().unsubscribe(this)
        super.onDestroy()
    }

    override fun onReadyStatusChanged(status: Boolean, mappResult: MappResult<DevicePayload>) {
        findViewById<Switch>(eu.brrm.shared_ui.R.id.switchReady).apply {
            isEnabled = false
            isChecked = status
        }
    }
}