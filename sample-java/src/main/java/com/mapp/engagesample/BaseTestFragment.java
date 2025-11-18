package com.mapp.engagesample;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.appoxee.Appoxee;
import com.appoxee.internal.model.response.DevicePayload;
import com.appoxee.internal.network.Call;
import com.appoxee.shared.AppoxeeObserver;
import com.appoxee.shared.GeoStatus;
import com.appoxee.shared.MappResult;
import com.google.android.material.button.MaterialButton;
import com.mapp.engagesample.inbox.InboxMessagesActivity;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import eu.brrm.shared_ui.Util;
import eu.brrm.shared_ui.attributes.get.GetCustomAttributesActivity;
import eu.brrm.shared_ui.attributes.set.SetCustomAttributesActivity;
import eu.brrm.shared_ui.databinding.FragmentBaseTestBinding;

public class BaseTestFragment extends Fragment {
    private static final String TAG = BaseTestFragment.class.getName();
    private final String alias = "abc1@maptest.com";
    private final Executor executor = Executors.newCachedThreadPool();
    private final Handler mainExecutor = new Handler(Looper.getMainLooper());
    private FragmentBaseTestBinding binding;
    private ClipboardManager clipboard;

    private final CompoundButton.OnCheckedChangeListener onPushEnabledListener = (materialButton, isChecked) -> pushEnable(isChecked);

    private final AppoxeeObserver appoxeeObserver = (status, mappResult) -> {
        Log.d(TAG, "SUCCESS IN BASE TEST FRAGMENT - Is Ready: " + status + "; Payload: " + mappResult.getData() + "; Error: " + mappResult.getError());
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

        clipboard = ContextCompat.getSystemService(requireContext(), ClipboardManager.class);

        binding.switchReady.setEnabled(false);

        binding.btnLogoutAndOptIn.setOnClickListener(v -> {
            logout(true);
        });

        binding.btnLogoutAndOptOut.setOnClickListener(v -> {
            logout(false);
        });

        binding.btnSetAlias.setOnClickListener(v -> {
            setAliasExecute();
        });

        binding.btnGetAlias.setOnClickListener(v -> {
            getAlias();
        });

        binding.btnGetDevice.setOnClickListener(v -> {
            getDevice();
        });

        binding.btnGetFbToken.setOnClickListener(v -> {
            getFbToken();
        });

        binding.btnFetchInboxMessages.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), InboxMessagesActivity.class);
            startActivity(intent);
        });

        binding.btnFetchInappMessages.setOnClickListener(v -> {
            Appoxee.instance().triggerInApp(requireActivity(), "app_open")
                    .enqueue(result -> {
                        if (!result.isSuccess()) {
                            Util.showDialog(requireContext(), "Error", result.getError().getMessage());
                        }
                    });
        });

        binding.btnSetTags.setOnClickListener(v -> {
            Appoxee.instance().addTags(Set.of("female", "makeup", "fashion")).enqueue(result -> {
                Util.showDialog(requireContext(), "Set Tags", String.valueOf(result.getData()));
            });
        });

        binding.btnRemoveTags.setOnClickListener(v -> {
            Appoxee.instance().removeTags(Set.of("female", "makeup", "fashion")).enqueue(result -> Util.showDialog(requireContext(), "Remove Tags", String.valueOf(result.getData())));
        });

        binding.btnGetTags.setOnClickListener(v -> {
            Appoxee.instance().getTags().enqueue(result -> {
                if (result.isSuccess()) {
                    StringBuilder sb = new StringBuilder();
                    List<String> tags = result.getData() != null ? result.getData() : Collections.emptyList();
                    for (String tag : tags) {
                        sb.append(tag);
                        if (tags.indexOf(tag) < tags.size() - 1) {
                            sb.append(", ");
                        }
                    }
                    Util.showDialog(requireContext(), "Tags", sb.toString());
                }
            });
        });

        binding.btnSetCustomAttributes.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SetCustomAttributesActivity.class);
            startActivity(intent);
        });

        binding.btnGetCustomAttributes.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), GetCustomAttributesActivity.class);
            startActivity(intent);
        });

        binding.btnStartGeofencing.setOnClickListener(v -> {
            startGeofencing();
        });

        binding.btnStopGeofencing.setOnClickListener(v -> {
            stopGeofencing();
        });

        binding.btnGetFbToken.setOnClickListener(v -> {
            getFirebaseToken();
        });

        binding.btnGeofencingStatus.setOnClickListener(v -> {
            checkGeofencingStatus();
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
        binding.switchReady.setChecked(status);
        binding.tvDevice.setText(sb.toString());
        if (!status) {
            Util.showDialog(requireContext(), "Error", "Mapp SDK not initialized!");
        }
    }

    private void logout(boolean pushEnabled) {
        Appoxee.instance().logout(pushEnabled).enqueue(result -> {
            if (result.isSuccess()) {
                String message = (pushEnabled) ? "Device successfully logged out with Opt in" : "Device successfully logged out with Opt Out";
                Util.showDialog(requireContext(), "Logout", message);
                updatePushEnabledState(pushEnabled);
            }
        });
    }

    private void updatePushEnabledState(boolean pushEnabled){
        binding.switchPushEnabled.setOnCheckedChangeListener(null);
        binding.switchPushEnabled.setChecked(pushEnabled);
        binding.switchPushEnabled.setOnCheckedChangeListener(onPushEnabledListener);
        binding.switchPushEnabled.setText((pushEnabled) ? "Opted In" : "Opted Out");
        binding.switchPushEnabled.setTextColor(Util.parseColor(requireContext(), Util.toColor(pushEnabled)));
    }

    private void setAliasExecute() {
        Editable editableAlias = binding.editTextAlias.getText();
        String alias = editableAlias != null ? editableAlias.toString() : "";
        if (alias.isBlank()) {
            Util.showDialog(requireContext(), "Set Alias Error", "Alias can't be empty. Please enter alias value!");
            return;
        }
        executor.execute(() -> {
            MappResult<String> result = Appoxee.instance().setAlias(alias, true).execute();
            requireActivity().runOnUiThread(() -> {
                if (result.isSuccess()) {
                    editableAlias.clear();
                    String dmcUserId = result.getData();
                    Util.showDialog(requireContext(), "DmcUserID", dmcUserId);
                } else {
                    String error = result.getError() != null ? result.getError().toString() : "Unknown error";
                    Util.showDialog(requireContext(), "Error", error);
                }
            });
        });
    }

    private void getAlias() {
        executor.execute(() -> {
            MappResult<String> mappResult = Appoxee.instance().getAlias().execute();
            if (mappResult.isSuccess()) {
                String alias = mappResult.getData();
                mainExecutor.post(() -> Util.showDialog(requireContext(), "Alias", alias));
            } else {
                String error = mappResult.getError() != null ? mappResult.getError().getMessage() : "Unknown error";
                mainExecutor.post(() -> Util.showDialog(requireContext(), "Error", error));
            }
        });
    }

    private void pushEnable(boolean enabled) {
        Call<Boolean> call = Appoxee.instance().enablePush(enabled, null);
        call.enqueue(result -> {
            boolean data = Boolean.TRUE.equals(result.getData());
            updatePushEnabledState(data);
            Util.showDialog(requireContext(), "Push Status", "ACTION " + (result.isSuccess() ? "SUCCESSFUL" : "UNSUCCESSFUL") + "\nStatus: " + enabled);
        });
    }

    private void getDevice() {
        Appoxee.instance().getDevice().enqueue(result -> {
            if (result.isSuccess()) {
                DevicePayload device = result.getData();
                Util.showDeviceInfoDialog(requireContext(), device, clipboard);
            } else {
                String error = result.getError() != null ? result.getError().toString() : "Unknown error";
                Util.showDialog(requireContext(), "Get Device Error", error);
            }
        });
    }

    private void getFbToken() {
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
    }

    private void isPushEnabled(@Nullable DevicePayload payload) {
        String pushToken = payload != null ? payload.getPushToken() : null;
        boolean enabled = pushToken != null && !pushToken.isEmpty();
        binding.switchPushEnabled.setOnCheckedChangeListener(null);
        binding.switchPushEnabled.setChecked(Boolean.TRUE.equals(enabled));
        binding.switchPushEnabled.setText(enabled ? "Opted In" : "Opted Out");
        binding.switchPushEnabled.setTextColor(getResources().getColor(Util.toColor(enabled)));
        binding.switchPushEnabled.setOnCheckedChangeListener(onPushEnabledListener);
    }

    private void handleLocationPermissionNotGranted() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Location permission needed")
                .setView(eu.brrm.shared_ui.R.layout.dialog_location_rationale)
                .setPositiveButton("Yes", (d, i) -> {
                    Uri uri = Uri.parse("package:" + requireContext().getPackageName());
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    d.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void startGeofencing() {
        Appoxee.instance().startGeofencing(0).enqueue(result -> {
            GeoStatus status = result.getData();
            Log.d(TAG, status.getClass().getName());
            if (status instanceof GeoStatus.GeoStartedOk) {
                Util.showDialog(requireContext(), "Geofencing Status", "Geofencing started successfully!");
            } else if (status instanceof GeoStatus.GeoLocationPermissionsNotGranted) {
                handleLocationPermissionNotGranted();
            } else {
                Util.showDialog(requireContext(), "Geofencing Status", status.getStatus());
            }
        });
    }

    private void stopGeofencing() {
        Appoxee.instance().stopGeofencing().enqueue(result -> {
            GeoStatus status = result.getData();
            if (status instanceof GeoStatus.GeoStoppedOk) {
                Util.showDialog(requireContext(), "Geofencing Status", "Geofencing stopped successfully!");
            } else {
                Util.showDialog(requireContext(), "Geofencing Status", status != null ? status.getStatus() : "N/A");
            }
        });
    }

    private void getFirebaseToken() {
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
    }

    private void checkGeofencingStatus() {
        Appoxee.instance().isGeofencingActive().enqueue(result -> {
            if (result.isSuccess()) {
                String message = Boolean.TRUE.equals(result.getData()) ? "Geofencing is active" : "Geofencing is inactive";
                Util.showDialog(requireContext(), "Geofencing Status", message);
            }
        });
    }
}
