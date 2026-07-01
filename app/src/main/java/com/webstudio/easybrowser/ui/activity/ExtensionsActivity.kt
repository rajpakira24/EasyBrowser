package com.webstudio.easybrowser.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.webstudio.easybrowser.ui.extensions.ExtensionsScreen
import com.webstudio.easybrowser.ui.extensions.ExtensionsViewModel

/**
 * Jetpack Compose extensions screen (Firefox-for-Android style): an "Enabled" section followed by
 * a "Recommended" list with ratings, review counts and one-tap install, plus "Find more
 * extensions". UI is in [ExtensionsScreen]; data/GeckoView bridging is in [ExtensionsViewModel].
 */
class ExtensionsActivity : ComponentActivity() {

    private val viewModel: ExtensionsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExtensionsScreen(
                viewModel = viewModel,
                onBack = { finish() },
                onOpenUrl = { url -> openInBrowser(url) },
            )
        }
    }

    private fun openInBrowser(url: String) {
        startActivity(
            Intent(this, BrowserActivity::class.java)
                .putExtra(BrowserActivity.EXTRA_URL, url)
        )
        finish()
    }
}
