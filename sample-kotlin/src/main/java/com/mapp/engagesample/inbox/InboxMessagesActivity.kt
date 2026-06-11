package com.mapp.engagesample.inbox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.appoxee.Appoxee
import com.appoxee.shared.MessageStatus
import com.appoxee.shared.InboxMessage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import eu.brrm.shared_ui.databinding.ActivityInboxMessagesBinding
import kotlinx.coroutines.launch

class InboxMessagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInboxMessagesBinding
    private lateinit var viewModel: InboxViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInboxMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[InboxViewModel::class.java]

        val adapter = InboxMessagesAdapter(
            onClick = { message, index->
                Appoxee.instance().showInboxMessage(this, message)
            },
            onLongClick = { message, index ->
                val items = arrayOf("Mark as read", "Mark as unread", "Mark as delete")
                MaterialAlertDialogBuilder(this)
                    .setItems(items) { dialog, which ->
                        val status = MessageStatus.entries[which]
                        viewModel.updateMessageStatus(message, index, status)
                    }.show()

            })
        binding.recycler.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stateUi.collect { state ->
                    binding.progressCircular.isVisible = state.isLoading
                    state.messages?.let {
                        adapter.submitList(state.messages)
                        val total =
                            "${getString(eu.brrm.shared_ui.R.string.total_message_count)} ${state.messages.size}"
                        binding.tvItemsCount.text = total
                    }
                    state.error?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}