package org.futo.voiceinput.settings

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.futo.voiceinput.R
import org.futo.voiceinput.Status
import org.futo.voiceinput.FloatingAssistantService
import org.futo.voiceinput.payments.BillingManager
import org.futo.voiceinput.settings.pages.AdvancedScreen
import org.futo.voiceinput.settings.pages.CreditsScreen
import org.futo.voiceinput.settings.pages.DependenciesScreen
import org.futo.voiceinput.settings.pages.HelpScreen
import org.futo.voiceinput.settings.pages.InputScreen
import org.futo.voiceinput.settings.pages.LanguagesScreen
import org.futo.voiceinput.settings.pages.ModelsScreen
import org.futo.voiceinput.settings.pages.PaymentFailedScreen
import org.futo.voiceinput.settings.pages.PaymentScreen
import org.futo.voiceinput.settings.pages.PaymentThankYouScreen
import org.futo.voiceinput.settings.pages.TestScreen
import org.futo.voiceinput.settings.pages.ThemeScreen


data class SettingsUiState(
    val intentResultText: String = "...",
    val numberOfResumes: Int = 0
)

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onResume() {
        _uiState.update { currentState ->
            currentState.copy(
                numberOfResumes = currentState.numberOfResumes + 1
            )
        }
    }

    fun onIntentResult(result: String) {
        _uiState.update { currentState ->
            currentState.copy(
                intentResultText = result
            )
        }
    }
}

fun Context.openSystemDefaultsSettings(component: ComponentName) {
    val uri = Uri.fromParts("package", component.packageName, null)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            startActivity(
                Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
                    data = uri
                }
            )

            return
        } catch(e: ActivityNotFoundException) {
            println("Failed to open ACTION_APP_OPEN_BY_DEFAULT_SETTINGS")
        }
    }

    try {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = uri
        })
    }catch(e: ActivityNotFoundException) {
        println("Failed to open ACTION_APPLICATION_DETAILS_SETTINGS")
    }
}

// פונקציית עזר הבודקת האם שירות פעיל לפי שם המחלקה כמחרוזת טקסט בלבד (מונע קריסות Classloader)
fun isServiceRunning(context: Context, className: String): Boolean {
    return try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        @Suppress("DEPRECATION")
        val services = am?.getRunningServices(100) ?: return false
        for (info in services) {
            if (info.service.className == className) {
                return true
            }
        }
        false
    } catch (e: Exception) {
        false
    }
}

// מימוש עצמאי, יציב וחסין-שגיאות לחלוטין של שורת הגדרה לחיצה (SettingLink)
@Composable
fun SettingLink(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "<", // חץ מעוצב RTL מותאם לעברית ללא תלות באייקונים חיצוניים
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// תצוגת מסך בית מופשטת ומעוצבת מחדש לחלוטין
@Composable
fun SimplifiedHomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    
    // מפתח השירות כמחרוזת טקסט למניעת צימוד (Decoupling) וקריסות בפתיחה
    val serviceClass = "org.futo.voiceinput.FloatingAssistantService"
    var isAssistantEnabled by remember { mutableStateOf(isServiceRunning(context, serviceClass)) }

    SettingListLazy {
        item {
            ScreenTitle("הגדרות", showBack = false, navController = navController)
        }

        // 1. שפות
        item {
            SettingLink(
                title = "שפות (Languages)",
                onClick = { navController.navigate("languages") }
            )
        }

        // 2. הגדרות קלט (Advanced)
        item {
            SettingLink(
                title = "הגדרות מתקדמות",
                onClick = { navController.navigate("advanced") }
            )
        }

        // 3. עזרה והדרכה
        item {
            SettingLink(
                title = "עזרה והדרכה",
                onClick = { navController.navigate("help") }
            )
        }

        // 4. אודות ומעקב בעיות (Credits)
        item {
            SettingLink(
                title = "אודות ומעקב בעיות",
                onClick = { navController.navigate("credits") }
            )
        }

        // 5. מתג הפעלת העוזר הקולי המבוסס טקסט בלבד למניעת קריסות בפתיחה
        item {
            SettingToggleRaw(
                "הפעלת העוזר הקולי (לחצן צף)",
                isAssistantEnabled,
                { active ->
                    isAssistantEnabled = active
                    val intent = Intent().setClassName(context.packageName, serviceClass)
                    if (active) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    } else {
                        context.stopService(intent)
                    }
                }
            )
        }
    }
}


@Composable
@Preview
fun SettingsMain(
    settingsViewModel: SettingsViewModel = viewModel(),
    navController: NavHostController = rememberNavController(),
    billing: BillingManager? = null
) {
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    val isAlreadyPaid = useDataStore(IS_ALREADY_PAID.key, default = IS_ALREADY_PAID.default)
    val hasSeenNotice = useDataStore(HAS_SEEN_PAID_NOTICE.key, default = HAS_SEEN_PAID_NOTICE.default)
    val paymentDest = if (!isAlreadyPaid.value && hasSeenNotice.value) {
        "error"
    } else if (isAlreadyPaid.value && !hasSeenNotice.value) {
        "paid"
    } else {
        "pleasePay"
    }

    LaunchedEffect(paymentDest) {
        if (paymentDest != "pleasePay") {
            navController.popBackStack("home", false)
            navController.navigate(
                paymentDest,
                NavOptions.Builder().setLaunchSingleTop(true).build()
            )
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // טעינת מסך הבית המופשט החדש
        composable("home") { SimplifiedHomeScreen(navController) }
        composable("advanced") { AdvancedScreen(settingsViewModel, navController) }
        composable("help") { HelpScreen(navController) }
        composable("languages") { LanguagesScreen(settingsViewModel, navController) }
        composable("testing") { TestScreen(settingsUiState.intentResultText, navController) }
        composable("models") { ModelsScreen(settingsViewModel, navController) }
        composable("input") { InputScreen(settingsViewModel, navController) }
        composable("themes") { ThemeScreen(navController) }

        composable("credits") {
            CreditsScreen(openDependencies = {
                navController.navigate("dependencies")
            }, navController = navController)
        }
        composable("dependencies") { DependenciesScreen(navController) }

        composable("pleasePay") {
            PaymentScreen(
                settingsViewModel,
                navController,
                { navController.navigateUp() },
                billing!!
            )
        }

        composable("paid") {
            PaymentThankYouScreen { navController.navigateUp() }
        }

        composable("error") {
            PaymentFailedScreen { navController.navigateUp() }
        }
    }
}

data class BlacklistedInputMethod(val packageName: String, val details: String, val dismiss: String)


@Composable
fun SetupOrMain(settingsViewModel: SettingsViewModel = viewModel(), billing: BillingManager) {
    val blacklistedMethods =
        listOf(
            BlacklistedInputMethod(
                "com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME",
                details = stringResource(R.string.gboard_incompatible_details),
                dismiss = stringResource(R.string.gboard_incompatible_accept)
            ),
            BlacklistedInputMethod(
                "ch.icoaching.typewise/ch.icoaching.wrio.Wrio",
                details = stringResource(R.string.typewise_incompatible_details),
                dismiss = stringResource(R.string.typewise_incompatible_accept)
            ),
            BlacklistedInputMethod(
                "com.samsung.android.honeyboard/.service.HoneyBoardService",
                details = stringResource(R.string.samsung_keyboard_incompatible_details),
                dismiss = stringResource(R.string.samsung_keyboard_incompatible_accept)
            ),
            BlacklistedInputMethod(
                "com.simplemobiletools.keyboard/.services.SimpleKeyboardIME",
                details = stringResource(R.string.simplekeyboard_incompatible_details),
                dismiss = stringResource(R.string.simplekeyboard_incompatible_accept)
            ),
            BlacklistedInputMethod(
                "rkr.simplekeyboard.inputmethod/.latin.LatinIME",
                details = stringResource(R.string.simplekeyboard_incompatible_details),
                dismiss = stringResource(R.string.simplekeyboard_incompatible_accept)
            )
        )

    val settingsUiState by settingsViewModel.uiState.collectAsState()

    val inputMethodEnabled = useIsInputMethodEnabled(settingsUiState.numberOfResumes)
    val microphonePermitted = useIsMicrophonePermitted(settingsUiState.numberOfResumes)
    val defaultIME = useDefaultIME(settingsUiState.numberOfResumes)

    val acknowledgedBlacklistedWarning = rememberSaveable { mutableStateOf(false) }
    val blacklistedKeyboardInfo =
        blacklistedMethods.firstOrNull { it.packageName == defaultIME.value }

    val acknowledgedWrongDefaultWarning = rememberSaveable { mutableStateOf(false) }
    val defaultVoiceInputIntent = useDefaultVoiceInputIntent(settingsUiState.numberOfResumes)

    if(defaultVoiceInputIntent.value.kind == DefaultVoiceInputIntentKind.OTHER && defaultVoiceInputIntent.value.name != null && !acknowledgedWrongDefaultWarning.value) {
        SetupWrongDefaultWarning(
            defaultVoiceInputIntent.value
        ) { acknowledgedWrongDefaultWarning.value = true }
    }else if (blacklistedKeyboardInfo != null && !acknowledgedBlacklistedWarning.value) {
        SetupBlacklistedKeyboardWarning(
            blacklistedKeyboardInfo
        ) { acknowledgedBlacklistedWarning.value = true }
    } else if (inputMethodEnabled.value == Status.False) {
        SetupEnableIME()
    } else if (microphonePermitted.value == Status.False) {
        SetupEnableMic()
    } else if ((inputMethodEnabled.value == Status.Unknown) || (microphonePermitted.value == Status.Unknown)) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterVertically)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    } else {
        SettingsMain(settingsViewModel, billing = billing)
    }
}
