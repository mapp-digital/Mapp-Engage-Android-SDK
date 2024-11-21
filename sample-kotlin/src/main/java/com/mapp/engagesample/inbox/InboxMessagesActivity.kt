package com.mapp.engagesample.inbox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import eu.brrm.shared_ui.databinding.ActivityInboxMessagesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InboxMessagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInboxMessagesBinding
    private lateinit var viewModel: InboxViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInboxMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[InboxViewModel::class.java]

        val adapter = InboxMessagesAdapter()
        binding.recycler.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messages.collect { state ->
                    withContext(Dispatchers.Main) {
                        binding.progressCircular.isVisible = state.isLoading
                        state.messages?.let {
                            adapter.submitList(state.messages)
                        }
                        state.error?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}