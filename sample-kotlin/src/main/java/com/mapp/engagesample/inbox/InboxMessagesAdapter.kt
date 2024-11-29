package com.mapp.engagesample.inbox

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import coil.transform.RoundedCornersTransformation
import com.appoxee.internal.model.response.inbox.InboxMessage
import eu.brrm.shared_ui.databinding.RowInboxMessageBinding

class InboxMessagesAdapter(
    private val onClick: ((InboxMessage, Int) -> Unit)? = null,
    private val onLongClick: ((InboxMessage, Int) -> Unit)? = null
) :
    ListAdapter<InboxMessage, MessageViewHolder>(InboxMessageDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return MessageViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.itemView.setOnClickListener {
            onClick?.invoke(getItem(position), position)
        }
        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(getItem(position), position)
            false
        }
    }
}


class InboxMessageDiffCallback() : DiffUtil.ItemCallback<InboxMessage>() {
    override fun areItemsTheSame(oldItem: InboxMessage, newItem: InboxMessage): Boolean {
        return oldItem.templateId == newItem.templateId && oldItem.status == newItem.status
    }

    override fun areContentsTheSame(oldItem: InboxMessage, newItem: InboxMessage): Boolean {
        return oldItem == newItem
    }
}

class MessageViewHolder(private val binding: RowInboxMessageBinding) : ViewHolder(binding.root) {
    fun bind(message: InboxMessage) {
        binding.tvTitle.text = message.subject
        binding.tvContent.text = message.summary
        binding.tvStatus.text = message.status.status
        binding.ivImage.load(message.iconUrl) {
            crossfade(true)
            transformations(RoundedCornersTransformation(10f))
        }
    }

    companion object {
        fun create(parent: ViewGroup): MessageViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = RowInboxMessageBinding.inflate(inflater, parent, false)
            return MessageViewHolder(binding)
        }
    }
}