package com.mapp.engagesample.inbox;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.appoxee.Appoxee;
import com.appoxee.internal.model.response.inbox.InboxMessage;
import com.appoxee.internal.model.response.inbox.MessageStatus;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import eu.brrm.shared_ui.databinding.ActivityInboxMessagesBinding;

public class InboxMessagesActivity extends AppCompatActivity {

    private ActivityInboxMessagesBinding binding;
    private InboxViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInboxMessagesBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(InboxViewModel.class);

        InboxMessagesAdapter adapter = getInboxMessagesAdapter();

        binding.recycler.setAdapter(adapter);

        viewModel.stateUi.observe(this, result -> {
            binding.progressCircular.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            if (!result.isLoading()) {
                if (result.getMessages() != null) {
                    List<InboxMessage> messages = result.getMessages();
                    adapter.submitList(messages);
                    Log.d("Inbox Messages", messages.toString());
                    String total = getString(eu.brrm.shared_ui.R.string.total_message_count) + messages.size();
                    binding.tvItemsCount.setText(total);
                } else {
                    String error = result.getError();
                    Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private @NonNull InboxMessagesAdapter getInboxMessagesAdapter() {
        InboxMessagesAdapter adapter = new InboxMessagesAdapter();

        final InboxMessagesAdapter.InboxActionsCallback callback = new InboxMessagesAdapter.InboxActionsCallback() {
            @Override
            public void onClick(InboxMessage message, int index) {
                Appoxee.instance().showInboxMessage(InboxMessagesActivity.this, message);
            }

            @Override
            public void onLongClick(InboxMessage message, int index) {
                String[] statuses = {"Mark as read", "Mark as unread", "Mark as deleted"};

                new MaterialAlertDialogBuilder(InboxMessagesActivity.this).setItems(statuses, (dialog, which) -> {
                    MessageStatus status = MessageStatus.getEntries().get(which);
                    viewModel.updateStatus(message, index, status);
                }).show();
            }
        };

        adapter.setCallback(callback);
        return adapter;
    }
}