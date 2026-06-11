package com.mapp.engagesample.inbox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.appoxee.shared.InboxMessage;
import com.bumptech.glide.Glide;

import java.util.Objects;

import eu.brrm.shared_ui.databinding.RowInboxMessageBinding;

public class InboxMessagesAdapter extends ListAdapter<InboxMessage, InboxMessagesAdapter.MessagesViewHolder> {

    public interface InboxActionsCallback {
        void onClick(InboxMessage message, int index);

        void onLongClick(InboxMessage message, int index);
    }

    private static final AsyncDifferConfig<InboxMessage> config = new AsyncDifferConfig.Builder<>(new DiffUtil.ItemCallback<InboxMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull InboxMessage oldItem, @NonNull InboxMessage newItem) {
            return oldItem.getTemplateId() == newItem.getTemplateId() &&
                    Objects.equals(oldItem.getStatus().getStatus(), newItem.getStatus().getStatus());
        }

        @Override
        public boolean areContentsTheSame(@NonNull InboxMessage oldItem, @NonNull InboxMessage newItem) {
            return Objects.equals(oldItem, newItem);
        }
    }).build();

    private InboxActionsCallback callback;

    protected InboxMessagesAdapter() {
        super(config);
    }

    public void setCallback(InboxActionsCallback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public MessagesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return MessagesViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull MessagesViewHolder holder, int position) {
        holder.bind(getItem(position));
        holder.itemView.setOnLongClickListener(v -> {
            callback.onLongClick(getItem(position), position);
            return false;
        });
        holder.itemView.setOnClickListener(v -> {
            callback.onClick(getItem(position), position);
        });
    }

    public static class MessagesViewHolder extends RecyclerView.ViewHolder {

        public MessagesViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void bind(InboxMessage message) {
            RowInboxMessageBinding binding = RowInboxMessageBinding.bind(itemView);
            binding.tvTitle.setText(message.getSubject());
            binding.tvContent.setText(message.getSummary());
            binding.tvStatus.setText(message.getStatus().getStatus());

            Glide.with(binding.ivImage)
                    .load(message.getIconUrl())
                    .fallback(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.stat_notify_error)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .into(binding.ivImage);

        }

        public static MessagesViewHolder create(@NonNull ViewGroup parent) {
            RowInboxMessageBinding binding = RowInboxMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new MessagesViewHolder(binding.getRoot());
        }
    }
}

