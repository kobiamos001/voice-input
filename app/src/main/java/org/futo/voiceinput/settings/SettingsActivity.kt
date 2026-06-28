package org.futo.voiceinput.settings

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.futo.voiceinput.R
import org.futo.voiceinput.payments.BillingManager
import org.futo.voiceinput.theme.UixThemeAuto
import org.futo.voiceinput.updates.scheduleUpdateCheckingJob

class SettingsActivity : ComponentActivity() {
    internal lateinit var billing: BillingManager
    private fun updateContent() {
        setContent {
            UixThemeAuto {
                // התאמת צבע הסטטוס בר לצבע הרקע של האפליקציה באופן דינמי
                val window = (LocalContext.current as? android.app.Activity)?.window
                if (window != null) {
                    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
                    val isBackgroundDark = !MaterialTheme.colorScheme.background.let { 
                        (it.red * 0.299 + it.green * 0.587 + it.blue * 0.114) > 0.5
                    }
                    SideEffect {
                        window.statusBarColor = backgroundColor
                        WindowCompat.getInsetsController(window, window.decorView).apply {
                            isAppearanceLightStatusBars = !isBackgroundDark
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SetupOrMain(billing = billing)
                }
            }
        }
    }

    private val permission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.onResume()
        }


    private val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    private val runVoiceIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.onIntentResult(
                when (it.resultCode) {
                    RESULT_OK -> {
                        val result =
                            it.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        if (result.isNullOrEmpty()) {
                            getString(R.string.intent_result_is_null_or_empty)
                        } else {
                            result[0]
                        }
                    }

                    RESULT_CANCELED -> getString(R.string.intent_was_cancelled)
                    else -> getString(R.string.unknown_intent_result)
                }
            )
        }

    internal fun requestPermission() {
        permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    internal fun launchVoiceIntent() {
        runVoiceIntent.launch(voiceIntent)
    }

    private lateinit var viewModel: SettingsViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        billing = BillingManager(this, lifecycleScope)

        viewModel = viewModels<SettingsViewModel>().value

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    updateContent()
                }
            }
        }

        scheduleUpdateCheckingJob(applicationContext)
    }

    override fun onStart() {
        super.onStart()

        billing.startConnection {
            it.checkAlreadyOwnsProduct()
        }
    }

    override fun onResume() {
        super.onResume()

        billing.onResume()
        viewModel.onResume()
    }

    override fun onRestart() {
        super.onRestart()

        billing.onResume()
        viewModel.onResume()
    }
}
