package com.mapp.engagesample;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.appoxee.Appoxee;
import com.appoxee.internal.model.response.DevicePayload;
import com.appoxee.internal.model.response.inapp.InappResponse;
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse;
import com.appoxee.shared.AppoxeeObserver;
import com.appoxee.shared.MappResult;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import eu.brrm.shared_ui.Util;
import eu.brrm.shared_ui.databinding.ActivityMainBinding;

/**
 * @noinspection ConstantValue, FieldCanBeLocal, RedundantSuppression
 */
@SuppressLint("UseSwitchCompatOrMaterialCode")
public class MainActivity extends AppCompatActivity implements AppoxeeObserver {

    private static final String TAG = MainActivity.class.getName();

    private final String alias = "abc1@maptest.com";

    private final Executor executor = Executors.newSingleThreadExecutor();

    private final Handler mainExecutor = new Handler(Looper.getMainLooper());

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.switchReady.setEnabled(false);

        binding.btnSetAlias.setOnClickListener(v -> {
            Editable alias = binding.editTextAlias.getText();
            if (alias == null || alias.toString().isEmpty()) {
                Util.showDialog(this, "Set alias", "Alias can not be empty!");
                return;
            }

            Appoxee.instance().setAlias(alias.toString()).enqueue(mappResult -> {
                if (mappResult.isSuccess()) {
                    alias.clear();
                }
                String dmcUserId = mappResult.getData();
                Util.showDialog(this, "DmcUserID", dmcUserId);
            });
        });

        binding.btnGetAlias.setOnClickListener(v -> {
            executor.execute(() -> {
                MappResult<String> mappResult = Appoxee.instance().getAlias().execute();
                String alias = mappResult.getData();
                mainExecutor.post(() -> Util.showDialog(this, "Alias", alias));
            });
        });

        binding.btnGetDevice.setOnClickListener(v -> {
            Appoxee.instance().getDevice().enqueue(mappResult -> {
                DevicePayload device = mappResult.getData();
                Util.showDialog(this, "Device", device != null ? device.toString() : "null");
            });
        });

        binding.btnFetchInboxMessages.setOnClickListener(v -> {
            Appoxee.instance().fetchInboxMessages("app_inbox").enqueue(mappResult -> {
                InboxMessagesResponse response = mappResult.getData();
                Util.showDialog(this, "Inbox Messages", response != null ? response.toString() : "");
            });
        });

        binding.btnFetchInappMessages.setOnClickListener(v -> {
            Appoxee.instance().fetchInappMessages("app_open").enqueue(mappResult -> {
                InappResponse response = mappResult.getData();
                Util.showDialog(this, "Inapp Messages", response != null ? response.toString() : "");
            });
        });

        binding.btnTestCallExecute.setOnClickListener(v -> {
            executor.execute(() -> {
                MappResult<String> response = Appoxee.instance().testCall().execute();
                mainExecutor.post(() -> Util.showDialog(this, "Response", response.getData()));
            });
        });

        binding.btnTestCallEnqueue.setOnClickListener(v -> {
            Appoxee.instance().testCall().enqueue(mappResult -> {
                Util.showDialog(this, "Response", mappResult.getData());
            });
        });

        binding.btnSetTags.setOnClickListener(v -> {
            Appoxee.instance().addTags(List.of("female", "makeup", "fashion")).enqueue(result -> {
                Util.showDialog(this, "Set Tags", String.valueOf(result.getData()));
            });
        });

        binding.btnRemoveTags.setOnClickListener(v -> {
            Appoxee.instance().removeTags(List.of("female", "makeup", "fashion")).enqueue(result -> Util.showDialog(this, "Remove Tags", String.valueOf(result.getData())));
        });

        binding.btnSetCustomAttributes.setOnClickListener(v -> {
            Appoxee.instance().addCustomAttributes(Map.of("currency", "EUR", "phone", "+381991234567")).enqueue(result -> Util.showDialog(this, "Set custom attribute", String.valueOf(result.getData())));
        });

        binding.btnGetCustomAttributes.setOnClickListener(v -> {
            Appoxee.instance().getCustomAttributes(List.of("currency", "phone")).enqueue(result -> {
                Util.showDialog(this, "Set custom attribute", String.valueOf(result.getData()));
            });
        });

        binding.btnTestInappEvent.setOnClickListener(v -> {
            Appoxee.instance().testInappEvent().enqueue(result -> {
                Util.showDialog(this, "Test Inapp Event", String.valueOf(result.getData()));
            });
        });

        binding.btnTestPushEvent.setOnClickListener(v -> {
            Appoxee.instance().testPushEvent().enqueue(result -> {
                Util.showDialog(this, "Test Push Event", String.valueOf(result.getData()));
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Appoxee.instance().subscribe(this);
    }

    @Override
    public void onReadyStatusChanged(boolean status, MappResult<DevicePayload> mappResult) {
        Log.d(TAG, "SUCCESS IN MAIN ACTIVITY - Is Ready: " + status + "; Payload: " + mappResult.getData() + "; Error: " + mappResult.getError());
        if (status) {
            updateUI(status, mappResult.getData());
        }
    }

    private void updateUI(boolean status, @Nullable DevicePayload payload) {
        Log.d(TAG, "UI Updating - Is Ready: " + status + "; Payload: " + payload);

        StringBuilder sb = new StringBuilder();
        if (payload != null) {
            sb.append("UDIDHashed: ").append("\n").append(payload.getUdidHashed());
        }
        getWindow().getDecorView().post(() -> {
            binding.switchReady.setChecked(status);
            binding.tvDevice.setText(sb.toString());
        });
    }

    private void setAlias() {
        executor.execute(() -> {
            MappResult<String> result = Appoxee.instance().setAlias(alias).execute();
            if (result.isSuccess()) {
                getDevice();
            }
        });

    }

    private void optIn() {
        executor.execute(() -> {
            String token = FirebaseMessaging.getInstance().getToken().getResult();
            Log.i(TAG, "PUSH TOKEN FROM APP: " + token);
            MappResult<Boolean> result = Appoxee.instance().optIn(token).execute();
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                getDevice();
            }
        });
    }

    private void getDevice() {
        MappResult<DevicePayload> result = Appoxee.instance().getDevice().execute();
        if (result.isSuccess()) {
            DevicePayload payload = result.getData();
            boolean ready = Appoxee.instance().isReady();
            mainExecutor.post(() -> updateUI(ready, payload));
        } else {
            Log.e(TAG, "ERROR IN MAIN ACTIVITY: " + result.getError());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Appoxee.instance().unsubscribe(this);
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }
}