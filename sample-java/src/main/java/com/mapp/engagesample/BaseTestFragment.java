package com.mapp.engagesample;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.appoxee.Appoxee;
import com.appoxee.internal.model.request.geo.GeoEvent;
import com.appoxee.internal.model.response.DevicePayload;
import com.appoxee.internal.model.response.geo.Region;
import com.appoxee.internal.model.response.inapp.InappResponse;
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse;
import com.appoxee.internal.network.Call;
import com.appoxee.shared.AppoxeeObserver;
import com.appoxee.shared.MappResult;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import eu.brrm.shared_ui.PermissionHelper;
import eu.brrm.shared_ui.Util;
import eu.brrm.shared_ui.databinding.FragmentBaseTestBinding;

public class BaseTestFragment extends Fragment implements AppoxeeObserver {
    private static final String TAG = BaseTestFragment.class.getName();
    private final String alias = "abc1@maptest.com";
    private final Executor executor = Executors.newCachedThreadPool();
    private final Handler mainExecutor = new Handler(Looper.getMainLooper());
    private FragmentBaseTestBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBaseTestBinding.inflate(getLayoutInflater());
        Appoxee.instance().subscribe(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestPostNotificationPermission();

        binding.switchReady.setEnabled(false);

        binding.btnSetAlias.setOnClickListener(v -> {
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
        });

        binding.btnGetAlias.setOnClickListener(v -> {
            executor.execute(() -> {
                MappResult<String> mappResult = Appoxee.instance().getAlias().execute();
                String alias = mappResult.getData();
                mainExecutor.post(() -> Util.showDialog(requireContext(), "Alias", alias));
            });
        });

        binding.btnGetDevice.setOnClickListener(v -> {
            Appoxee.instance().getDevice().enqueue(mappResult -> {
                DevicePayload device = mappResult.getData();
                Util.showDialog(requireContext(), "Device", device != null ? device.toString() : "null");
            });
//            executor.execute(() -> {
//                MappResult<DevicePayload> mappResult = Appoxee.instance().getDevice().execute();
//                String device=mappResult.getData()!=null ? mappResult.getData().toString() : "";
//                mainExecutor.post(() -> Util.showDialog(requireContext(), "Device", device));
//            });
        });

        binding.btnFetchInboxMessages.setOnClickListener(v -> {
            Appoxee.instance().fetchInboxMessages("app_inbox").enqueue(mappResult -> {
                InboxMessagesResponse response = mappResult.getData();
                Util.showDialog(requireContext(), "Inbox Messages", response != null ? response.toString() : "");
            });
        });

        binding.btnFetchInappMessages.setOnClickListener(v -> {
            Appoxee.instance().fetchInappMessages("app_open").enqueue(mappResult -> {
                InappResponse response = mappResult.getData();
                Util.showDialog(requireContext(), "Inapp Messages", response != null ? response.toString() : "");
            });
        });

        binding.btnTestCallExecute.setOnClickListener(v -> {
            executor.execute(() -> {
                MappResult<String> response = Appoxee.instance().testCall().execute();
                mainExecutor.post(() -> Util.showDialog(requireContext(), "Response", response.getData()));
            });
        });

        binding.btnTestCallEnqueue.setOnClickListener(v -> {
            Appoxee.instance().testCall().enqueue(mappResult -> {
                Util.showDialog(requireContext(), "Response", mappResult.getData());
            });
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

        binding.btnTestInappEvent.setOnClickListener(v -> {
            Appoxee.instance().testInappEvent().enqueue(result -> {
                Util.showDialog(requireContext(), "Test Inapp Event", String.valueOf(result.getData()));
            });
        });

        binding.btnTestPushEvent.setOnClickListener(v -> {
            Appoxee.instance().testPushEvent().enqueue(result -> {
                Util.showDialog(requireContext(), "Test Push Event", String.valueOf(result.getData()));
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

        binding.btnTestActivate.setOnClickListener(v -> {
            Call<Boolean> call = Appoxee.instance().testActivate();
            call.enqueue(result -> {
                Util.showDialog(requireContext(), "Activate", String.valueOf(result.getData()));
            });
        });

        binding.btnGetFbToken.setOnClickListener(v -> {
            Appoxee.instance().getFirebaseToken().enqueue(result -> {
                if (!result.isSuccess() || result.getData() == null) return;

                String token = result.getData();
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
        Appoxee.instance().subscribe(this);
        for (int i = 0; i < binding.llInnerContainer.getChildCount(); i++) {
            View child = binding.llInnerContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                child.setEnabled(child.hasOnClickListeners());
            }
        }
    }

    private void requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionHelper permissionHelper = new PermissionHelper(requireActivity().getActivityResultRegistry());
            List<String> permissions = new ArrayList<>();
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            permissionHelper.requestPermissions(requireContext(), permissions, result -> {
                Toast.makeText(requireContext(), "Permission(s) granted: \n" + Util.permissionsToString(result), Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        Appoxee.instance().unsubscribe(this);
        super.onDestroyView();
    }

    @Override
    public void onReadyStatusChanged(boolean status, @NonNull MappResult<DevicePayload> mappResult) {
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

        isPushEnabled();

        mainExecutor.post(() -> {
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

    private void pushEnable(boolean enabled) {
        Call<Boolean> call = Appoxee.instance().enablePush(enabled, null);
        call.enqueue(result -> {
            boolean data = Boolean.TRUE.equals(result.getData());
            Util.showDialog(requireContext(), "Push Status", "ACTION " + (data ? "SUCCESSFUL" : "UNSUCCESSFUL") + "\nStatus: " + enabled);
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

    private void isPushEnabled() {
        Call<Boolean> call = Appoxee.instance().isPushEnabled();
        call.enqueue(result -> {
            if (result.isSuccess()) {
                Boolean enabled = result.getData();
                binding.switchPushEnabled.setOnCheckedChangeListener(null);
                binding.switchPushEnabled.setChecked(Boolean.TRUE.equals(enabled));
                binding.switchPushEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    pushEnable(isChecked);
                });
            }
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
