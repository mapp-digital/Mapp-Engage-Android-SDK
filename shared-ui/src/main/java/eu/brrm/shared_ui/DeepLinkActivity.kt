package eu.brrm.shared_ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import eu.brrm.shared_ui.databinding.ActivityDeepLinkBinding

class DeepLinkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeepLinkBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeepLinkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.let {
            val data = it.data ?: return@let
            val scheme = data.scheme
            val authority = data.authority
            val link = data.getQueryParameter("link")
            val messageId = data.getQueryParameter("messageId")?.toLongOrNull()

            val sb = StringBuffer().apply {
                append("scheme: ").append(scheme).append("\n")
                append("authority: ").append(authority).append("\n")
                append("path: ").append(link).append("\n")
                append("messageId: ").append(messageId)
            }
            binding.tvDeepLinkUri.text = sb.toString()
            binding.btnOpenDeepLink.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                startActivity(intent)
            }
        }
    }
}