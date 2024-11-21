package com.mapp.engagesample;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.appoxee.Appoxee;
import com.appoxee.internal.model.request.geo.GeoEvent;
import com.appoxee.internal.model.response.DevicePayload;
import com.appoxee.internal.model.response.geo.Region;
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse;
import com.appoxee.internal.network.Call;
import com.appoxee.shared.AppoxeeObserver;
import com.appoxee.shared.MappResult;
import com.google.android.material.button.MaterialButton;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import eu.brrm.shared_ui.Util;
import eu.brrm.shared_ui.databinding.FragmentBaseTestBinding;

public class BaseTestFragment extends Fragment {
    private static final String TAG = BaseTestFragment.class.getName();
    private final String alias = "abc1@maptest.com";
    private final Executor executor = Executors.newCachedThreadPool();
    private final Handler mainExecutor = new Handler(Looper.getMainLooper());
    private FragmentBaseTestBinding binding;


    private final AppoxeeObserver appoxeeObserver = (status, mappResult) -> {
        Log.d(TAG, "SUCCESS IN MAIN ACTIVITY - Is Ready: " + status + "; Payload: " + mappResult.getData() + "; Error: " + mappResult.getError());
        if (status) {
            updateUI(status, mappResult.getData());
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBaseTestBinding.inflate(getLayoutInflater());
        Appoxee.instance().subscribe(appoxeeObserver);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.switchReady.setEnabled(false);

        binding.btnSetAlias.setOnClickListener(v -> {
            setAlias();
        });

        binding.btnGetAlias.setOnClickListener(v -> {
            getAlias();
        });

        binding.btnGetDevice.setOnClickListener(v -> {
            getDevice();
        });

        binding.btnFetchInboxMessages.setOnClickListener(v -> {
            Appoxee.instance().fetchInboxMessages().enqueue(mappResult -> {
                InboxMessagesResponse response = mappResult.getData();
                Util.showDialog(requireContext(), "Inbox Messages", response != null ? response.toString() : "");
            });
        });

        binding.btnFetchInappMessages.setOnClickListener(v -> {
            Appoxee.instance().triggerInApp(requireActivity(), "app_open");
        });

        binding.btnSetTags.setOnClickListener(v -> {
            Appoxee.instance().addTags(List.of("female", "makeup", "fashion")).enqueue(result -> {
                Util.showDialog(requireContext(), "Set Tags", String.valueOf(result.getData()));
            });
        });

        binding.btnRemoveTags.setOnClickListener(v -> {
            Appoxee.instance().removeTags(List.of("female", "makeup", "fashion")).enqueue(result -> Util.showDialog(requireContext(), "Remove Tags", String.valueOf(result.getData())));
        });

        binding.btnSetCustomAttributes.setOnClickListener(v -> {
            Appoxee.instance().addCustomAttributes(Map.of("currency", "EUR", "phone", "+381991234567")).enqueue(result -> Util.showDialog(requireContext(), "Set custom attribute", String.valueOf(result.getData())));
        });

        binding.btnGetCustomAttributes.setOnClickListener(v -> {
            Appoxee.instance().getCustomAttributes(List.of("currency", "phone")).enqueue(result -> {
                Util.showDialog(requireContext(), "Set custom attribute", String.valueOf(result.getData()));
            });
        });

        binding.btnGetRegions.setOnClickListener(v -> {
            Appoxee.instance().testGetRegions(43.1407, 20.5181, 0, 50).enqueue(result -> {
                List<Region> regions = result.getData() != null ? result.getData().getRegions() : Collections.emptyList();
                StringBuilder sb = new StringBuilder();
                for (Region r : regions) {
                    sb.append("(").append(r.getId()).append(") ");
                    sb.append(r.getName()).append("\n(").append(r.getLat()).append("/").append(r.getLng()).append(")\n\n");
                }
                Util.showDialog(requireContext(), "Regions", sb.toString());
            });
        });

        binding.btnEventRegions.setOnClickListener(v -> {
            Appoxee.instance().testRegionEvent(GeoEvent.ENTER, 43.1407, 20.5181, 91, 0).enqueue(result -> {
                Util.showDialog(requireContext(), "Trigger Enter Geolocation", String.valueOf(result.getData()));
            });
        });

        binding.btnGetFbToken.setOnClickListener(v -> {
            Appoxee.instance().getFirebaseToken().enqueue(result -> {
                if (!result.isSuccess() || result.getData() == null) return;

                String token = result.getData();
                Log.d(TAG, "FIREBASE TOKEN: " + token);
                // copy token to clipboard
                ClipboardManager clipboard = ContextCompat.getSystemService(
                        requireContext(),
                        ClipboardManager.class
                );
                ClipData clip = ClipData.newPlainText("token", token);
                if (clip != null && clipboard != null) {
                    clipboard.setPrimaryClip(clip);

                    // show dialog with token value
                    Util.showDialog(requireContext(), "Firebase token", token);
                }
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        for (int i = 0; i < binding.llInnerContainer.getChildCount(); i++) {
            View child = binding.llInnerContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                child.setEnabled(child.hasOnClickListeners());
            }
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        Appoxee.instance().unsubscribe(appoxeeObserver);
        super.onDestroyView();
    }

    private void updateUI(boolean status, @Nullable DevicePayload payload) {
        Log.d(TAG, "UI Updating - Is Ready: " + status + "; Payload: " + payload);

        StringBuilder sb = new StringBuilder();
        if (payload != null) {
            sb.append("UDIDHashed: ").append("\n").append(payload.getUdidHashed());
        }

        isPushEnabled(payload);

        mainExecutor.post(() -> {
            binding.switchReady.setChecked(status);
            binding.tvDevice.setText(sb.toString());
        });
    }

    private void setAlias() {
        Editable alias = binding.editTextAlias.getText();
        if (alias == null || alias.toString().isEmpty()) {
            Util.showDialog(requireContext(), "Set alias", "Alias can not be empty!");
            return;
        }

        Appoxee.instance().setAlias(alias.toString()).enqueue(mappResult -> {
            if (mappResult.isSuccess()) {
                alias.clear();
            }
            String dmcUserId = mappResult.getData();
            Util.showDialog(requireContext(), "DmcUserID", dmcUserId);
        });
    }

    private void getAlias() {
        executor.execute(() -> {
            MappResult<String> mappResult = Appoxee.instance().getAlias().execute();
            String alias = mappResult.getData();
            mainExecutor.post(() -> Util.showDialog(requireContext(), "Alias", alias));
        });
    }

    private void pushEnable(boolean enabled) {
        Call<Boolean> call = Appoxee.instance().enablePush(enabled, null);
        call.enqueue(result -> {
            boolean data = Boolean.TRUE.equals(result.getData());
            Util.showDialog(requireContext(), "Push Status", "ACTION " + (data ? "SUCCESSFUL" : "UNSUCCESSFUL") + "\nStatus: " + enabled);
        });
    }

    private void getDevice() {
        Appoxee.instance().getDevice().enqueue(mappResult -> {
            DevicePayload device = mappResult.getData();
            Util.showDialog(requireContext(), "Device", device != null ? device.toString() : "null");
        });
    }

    private void isPushEnabled(@Nullable DevicePayload payload) {
        String pushToken = payload != null ? payload.getPushToken() : null;
        Boolean enabled = pushToken != null && !pushToken.isEmpty();
        binding.switchPushEnabled.setOnCheckedChangeListener(null);
        binding.switchPushEnabled.setChecked(Boolean.TRUE.equals(enabled));
        binding.switchPushEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pushEnable(isChecked);
        });
    }

    private void setToken() {
        Call<String> call = Appoxee.instance().getFirebaseToken();
        call.enqueue(result -> {
            if (result.isSuccess()) {
                String token = result.getData();
            }
        });
    }
}
