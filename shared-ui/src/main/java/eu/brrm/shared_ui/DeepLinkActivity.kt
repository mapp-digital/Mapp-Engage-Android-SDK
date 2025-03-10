package eu.brrm.shared_ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appoxee.internal.Actions
import eu.brrm.shared_ui.databinding.ActivityDeepLinkBinding

class DeepLinkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeepLinkBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeepLinkBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handleIntent(intent)
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action ?: return
        val data = intent.data ?: return
        val scheme = data.scheme
        val authority = data.authority

        if (action == Actions.MAPP_DEEP_LINK_ACTION &&
            scheme == Actions.MAPP_DEEP_LINK_SCHEME &&
            authority == Actions.MAPP_DEEP_LINK_AUTHORITY
        ) {
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
                val openingIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                if (openingIntent.resolveActivity(this.packageManager) != null) {
                    startActivity(openingIntent)
                    this@DeepLinkActivity.finishAfterTransition()
                } else {
                    Util.showDialog(
                        this,
                        "Unsupported intent",
                        "No activity can handle provided deeplink!"
                    )
                }
            }
        }
    }
}