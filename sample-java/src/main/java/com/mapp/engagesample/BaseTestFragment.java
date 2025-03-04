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

        binding.switchReady.setEnabled(false);

        binding.btnSetAlias.setOnClickListener(v -> {
            setAliasExecute();
        });

        binding.btnGetAlias.setOnClickListener(v -> {
            getAlias();
        });

        binding.btnGetDevice.setOnClickListener(v -> {
            getDevice();
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

        binding.btnStartGeofencing.setOnClickListener(v -> {
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
        });

        binding.btnStopGeofencing.setOnClickListener(v -> {
            Appoxee.instance().stopGeofencing().enqueue(result -> {
                GeoStatus status = result.getData();
                if (status instanceof GeoStatus.GeoStoppedOk) {
                    Util.showDialog(requireContext(), "Geofencing Status", "Geofencing stopped successfully!");
                } else {
                    Util.showDialog(requireContext(), "Geofencing Status", status != null ? status.getStatus() : "N/A");
                }
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
        binding.switchReady.setChecked(status);
        binding.tvDevice.setText(sb.toString());
        if (!status) {
            Util.showDialog(requireContext(), "Error", "Mapp SDK not initialized!");
        }
    }

    private void setAlias() {
        Editable editableAlias = binding.editTextAlias.getText();
        String alias = editableAlias != null ? editableAlias.toString() : null;
        Appoxee.instance().setAlias(alias).enqueue(result -> {
            if (result.isSuccess()) {
                if (editableAlias != null) {
                    editableAlias.clear();
                }
                String dmcUserId = result.getData();
                Util.showDialog(requireContext(), "DmcUserID", dmcUserId);
            } else {
                String error = result.getError() != null ? result.getError().toString() : "Unknown error";
                Util.showDialog(requireContext(), "Error", error);
            }

        });
    }

    private void setAliasExecute() {
        Editable editableAlias = binding.editTextAlias.getText();
        String alias = editableAlias != null ? editableAlias.toString() : null;
        executor.execute(() -> {
            MappResult<String> result = Appoxee.instance().setAlias(alias).execute();
            requireActivity().runOnUiThread(()->{
                if (result.isSuccess()) {
                    if (editableAlias != null) {
                        editableAlias.clear();
                    }
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
            binding.switchPushEnabled.setText((data) ? "Opted In" : "Opted Out");
            binding.switchPushEnabled.setTextColor(getResources().getColor(Util.toColor(data)));
            Util.showDialog(requireContext(), "Push Status", "ACTION " + (result.isSuccess() ? "SUCCESSFUL" : "UNSUCCESSFUL") + "\nStatus: " + enabled);
        });
    }

    private void getDevice() {
        Appoxee.instance().getDevice().enqueue(result -> {
            if (result.isSuccess()) {
                DevicePayload device = result.getData();
                Util.showDialog(requireContext(), "Device", device != null ? device.toString() : "null");
            } else {
                String error = result.getError() != null ? result.getError().toString() : "Unknown error";
                Util.showDialog(requireContext(), "Get Device Error", error);
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
        binding.switchPushEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pushEnable(isChecked);
        });
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

    private void setToken() {
        Call<String> call = Appoxee.instance().getFirebaseToken();
        call.enqueue(result -> {
            if (result.isSuccess()) {
                String token = result.getData();
            }
        });
    }
}
