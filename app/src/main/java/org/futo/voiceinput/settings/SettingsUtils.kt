package org.futo.voiceinput.settings

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import org.futo.voiceinput.MULTILINGUAL_MODELS
import org.futo.voiceinput.startModelDownloadActivity
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

// פונקציית עזר הבודקת האם שירות פעיל לפי שם המחלקה כמחרוזת טקסט בלבד
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

// כותרת קטגוריה בעיצוב מודרני מוגדל (ללא הדגשה שבוטלה)
@Composable
fun SettingCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        textAlign = TextAlign.Start
    )
}

// פונקציית קישור מודרנית עם תמיכה בכותרת משנה (Subtitle) ואייקון מקומי מתיקיית ה-Drawable
@Composable
fun SettingLink(
    title: String,
    subtitle: String? = null,
    @DrawableRes iconRes: Int? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// שחזור חתימת הפונקציה המקורית לתאימות מלאה עם קריאות חיצוניות
@Composable
fun SettingLink(
    title: String,
    onClick: () -> Unit
) {
    SettingLink(title = title, subtitle = null, iconRes = null, onClick = onClick)
}

// פונקציית מתג (Toggle) מודרנית עם תמיכה בכותרת משנה ואייקון מקומי מתיקיית ה-Drawable
@Composable
fun ModernSettingToggle(
    title: String,
    subtitle: String? = null,
    @DrawableRes iconRes: Int? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

// תצוגת מסך בית מופשטת ומעוצבת מחדש
@Composable
fun SimplifiedHomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    
    val serviceClass = "org.futo.voiceinput.FloatingAssistantService"
    var isAssistantEnabled by remember { mutableStateOf(isServiceRunning(context, serviceClass)) }

    // שימוש בהגדרת ה-VAD ישירות במסך הבית
    val (isVadEnabled, setVadEnabled) = useDataStore(IS_VAD_ENABLED.key, default = IS_VAD_ENABLED.default)

    // שליטה במצב השפות והסנכרון של המודלים
    val (multilingual, setMultilingual) = useDataStore(ENABLE_MULTILINGUAL)
    val (multilingualModelIndex, _) = useDataStore(MULTILINGUAL_MODEL_INDEX)
    val (languages, setLanguages) = useDataStore(LANGUAGE_TOGGLES)

    val prefs = remember { context.getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAssistantTypeDialog by remember { mutableStateOf(false) }

    // טעינת סוג העוזר הנבחר
    var assistantType by remember { mutableStateOf(prefs.getString("assistant_type", "floating") ?: "floating") }

    LaunchedEffect(listOf(multilingualModelIndex, multilingual)) {
        if (multilingual) {
            context.startModelDownloadActivity(listOf(MULTILINGUAL_MODELS[multilingualModelIndex]))
        }
    }

    LaunchedEffect(languages) {
        val userChoseEnglish = prefs.getBoolean("user_chose_english", false)

        if (languages.isEmpty() || (languages.size == 1 && languages.contains("en") && !userChoseEnglish)) {
            setLanguages(setOf("he"))
        }

        val newMultilingual = languages.count { it != "en" } > 0
        if (multilingual != newMultilingual) setMultilingual(newMultilingual)
    }

    SettingListLazy {
        item {
            ScreenTitle("העדפות", showBack = false, navController = navController)
        }

        // 1. מקטע ראשון: עוזר קולי (הועלה למעלה)
        item {
            SettingCategoryHeader("עוזר קולי")
        }
        item {
            ModernSettingToggle(
                title = "הפעלת שירות עוזר קולי",
                subtitle = "הפעלת לחצן צף או שירות רקע לגישה מהירה",
                iconRes = R.drawable.ic_keyboard_voice,
                checked = isAssistantEnabled,
                onCheckedChange = { active ->
                    val intent = Intent().setClassName(context.packageName, serviceClass)
                    if (active) {
                        if (!Settings.canDrawOverlays(context)) {
                            isAssistantEnabled = false
                            try {
                                val overlayIntent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(overlayIntent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            isAssistantEnabled = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        }
                    } else {
                        isAssistantEnabled = false
                        context.stopService(intent)
                    }
                }
            )
        }
        item {
            SettingLink(
                title = "סוג עוזר קולי",
                subtitle = if (assistantType == "floating") "לחצן צף" else "התראה בשורת ההתראות",
                iconRes = R.drawable.ic_settings_suggest,
                onClick = { showAssistantTypeDialog = true }
            )
        }

        // 2. מקטע שני: הקלדה קולית (הורד למטה)
        item {
            SettingCategoryHeader("הקלדה קולית")
        }
        item {
            SettingLink(
                title = "שפות (Languages)",
                subtitle = if (languages.contains("en")) "אנגלית (English)" else "עברית (ברירת מחדל)",
                iconRes = R.drawable.ic_language,
                onClick = { showLanguageDialog = true }
            )
        }
        item {
            ModernSettingToggle(
                title = "עצירה אוטומטית בשקט (עבור מקלדת)",
                subtitle = "עצירת ההקלטה באופן אוטומטי כאשר מזוהה שקט",
                iconRes = R.drawable.ic_hearing,
                checked = isVadEnabled,
                onCheckedChange = { active -> setVadEnabled(active) }
            )
        }

        // 3. מקטע שלישי: עזרה
        item {
            SettingCategoryHeader("עזרה")
        }
        item {
            SettingLink(
                title = "עזרה והדרכה",
                subtitle = "מדריכים ופתרון בעיות נפוצות",
                iconRes = R.drawable.ic_help_outline,
                onClick = { navController.navigate("help") }
            )
        }
        item {
            SettingLink(
                title = "אודות ומעקב בעיות",
                subtitle = "מידע על האפליקציה, רישיונות ודיווח על תקלות",
                iconRes = R.drawable.ic_info,
                onClick = { showAboutDialog = true }
            )
        }
    }

    // תיבת דו-שיח מודרנית לבחירת שפה
    if (showLanguageDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showLanguageDialog = false }
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "בחירת שפה",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // אופציה 1: עברית
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                prefs.edit().putBoolean("user_chose_english", false).apply()
                                setLanguages(setOf("he"))
                                showLanguageDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "עברית",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "עברית (ברירת מחדל)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = languages.contains("he"),
                            onClick = {
                                prefs.edit().putBoolean("user_chose_english", false).apply()
                                setLanguages(setOf("he"))
                                showLanguageDialog = false
                            }
                        )
                    }

                    // אופציה 2: אנגלית
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                prefs.edit().putBoolean("user_chose_english", true).apply()
                                setLanguages(setOf("en"))
                                showLanguageDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "אנגלית (English)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Trained on english language models",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = languages.contains("en"),
                            onClick = {
                                prefs.edit().putBoolean("user_chose_english", true).apply()
                                setLanguages(setOf("en"))
                                showLanguageDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    // תיבת דו-שיח מודרנית לבחירת סוג העוזר
    if (showAssistantTypeDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAssistantTypeDialog = false }
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "סוג עוזר קולי",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // אופציה 1: לחצן צף
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                prefs.edit().putString("assistant_type", "floating").apply()
                                assistantType = "floating"
                                showAssistantTypeDialog = false
                                if (isAssistantEnabled) {
                                    val intent = Intent().setClassName(context.packageName, serviceClass)
                                    context.stopService(intent)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "לחצן צף",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "הצגת לחצן צף קטן ונוח על גבי המסך",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = assistantType == "floating",
                            onClick = {
                                prefs.edit().putString("assistant_type", "floating").apply()
                                assistantType = "floating"
                                showAssistantTypeDialog = false
                                if (isAssistantEnabled) {
                                    val intent = Intent().setClassName(context.packageName, serviceClass)
                                    context.stopService(intent)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                }
                            }
                        )
                    }

                    // אופציה 2: התראה בשורת ההתראות
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                prefs.edit().putString("assistant_type", "notification").apply()
                                assistantType = "notification"
                                showAssistantTypeDialog = false
                                if (isAssistantEnabled) {
                                    val intent = Intent().setClassName(context.packageName, serviceClass)
                                    context.stopService(intent)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "התראה בשורת ההתראות",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "שליטה בהקלטה וביצוע הפעולות ישירות מווילון ההתראות",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = assistantType == "notification",
                            onClick = {
                                prefs.edit().putString("assistant_type", "notification").apply()
                                assistantType = "notification"
                                showAssistantTypeDialog = false
                                if (isAssistantEnabled) {
                                    val intent = Intent().setClassName(context.packageName, serviceClass)
                                    context.stopService(intent)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // תיבת דו-שיח מודרנית עבור אודות ויצירת קשר התואמת לעיצוב ה-XML
    if (showAboutDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAboutDialog = false }
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // לוגו האפליקציה (72dp)
                    Icon(
                        painter = painterResource(id = R.drawable.ic_keyboard_voice),
                        contentDescription = "App Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // שם האפליקציה
                    Text(
                        text = "HTransfer",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // גרסה
                    Text(
                        text = "V2.1",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // חתימת היוצר Build By HPower
                    Text(
                        text = "Build By HPower",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "©",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // כרטיס בועת יצירת קשר מעוגל
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // שורת אתר האינטרנט
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://kobiamos001.github.io"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_language),
                                    contentDescription = "Website Icon",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "https://kobiamos001.github.io",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // שורת אימייל
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:kobiamos001@gmail.com"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_email),
                                    contentDescription = "Email Icon",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "kobiamos001@gmail.com",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
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
