package org.futo.voiceinput.settings.pages

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Job
import org.futo.voiceinput.LANGUAGE_LIST
import org.futo.voiceinput.MULTILINGUAL_MODELS
import org.futo.voiceinput.R
import org.futo.voiceinput.settings.ENABLE_MULTILINGUAL
import org.futo.voiceinput.settings.LANGUAGE_TOGGLES
import org.futo.voiceinput.settings.MULTILINGUAL_MODEL_INDEX
import org.futo.voiceinput.settings.ScreenTitle
import org.futo.voiceinput.settings.SettingListLazy
import org.futo.voiceinput.settings.SettingToggleRaw
import org.futo.voiceinput.settings.SettingsViewModel
import org.futo.voiceinput.settings.Tip
import org.futo.voiceinput.settings.useDataStore
import org.futo.voiceinput.startModelDownloadActivity


@Composable
fun LanguageToggle(
    id: String,
    name: String,
    languages: Set<String>,
    setLanguages: (Set<String>) -> Job,
    subtitle: String?
) {
    // מונע מהמשתמש לכבות את השפה הפעילה היחידה (חייבת להישאר תמיד לפחות שפה אחת פעילה)
    val disabled = languages.contains(id) && languages.size == 1

    SettingToggleRaw(
        name,
        languages.contains(id),
        { isChecked ->
            if (isChecked) {
                // הדלקה של שפה זו תגדיר אותה כשפה הפעילה היחידה ותכבה את השניה
                setLanguages(setOf(id))
            }
        },
        subtitle = if(disabled) { stringResource(R.string.only_language_enabled) } else { subtitle },
        disabled = disabled
    )
}

@Composable
@Preview
fun LanguagesScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val (multilingual, setMultilingual) = useDataStore(ENABLE_MULTILINGUAL)
    val (multilingualModelIndex, _) = useDataStore(MULTILINGUAL_MODEL_INDEX)
    val (languages, setLanguages) = useDataStore(LANGUAGE_TOGGLES)
    val context = LocalContext.current

    LaunchedEffect(listOf(multilingualModelIndex, multilingual)) {
        if (multilingual) {
            context.startModelDownloadActivity(listOf(MULTILINGUAL_MODELS[multilingualModelIndex]))
        }
    }

    LaunchedEffect(languages) {
        val prefs = context.getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)
        
        // קריאת המצב הנוכחי של הדגל
        var userChoseEnglish = prefs.getBoolean("user_chose_english", false)

        if (languages.size == 1 && languages.contains("en")) {
            prefs.edit().putBoolean("user_chose_english", true).apply()
            userChoseEnglish = true // עדכון מקומי מיידי של המשתנה למניעת תחרות ריצה
        } else if (languages.contains("he")) {
            prefs.edit().putBoolean("user_chose_english", false).apply()
            userChoseEnglish = false // עדכון מקומי מיידי של המשתנה למניעת תחרות ריצה
        }

        // כעת הבדיקה מתבצעת מול המצב המעודכן באותו הרגע
        if (languages.isEmpty() || (languages.size == 1 && languages.contains("en") && !userChoseEnglish)) {
            setLanguages(setOf("he"))
        }

        val newMultilingual = languages.count { it != "en" } > 0
        if (multilingual != newMultilingual) setMultilingual(newMultilingual)
    }

    // הצגת עברית ואנגלית בלבד ברשימה
    val filteredLanguages = LANGUAGE_LIST.filter { it.id == "he" || it.id == "en" }

    SettingListLazy {
        item {
            ScreenTitle(stringResource(R.string.languages_title), showBack = true, navController = navController)
        }

        item {
            Tip(stringResource(R.string.language_tip_1))
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(filteredLanguages.size) {
            val language = filteredLanguages[it]

            val subtitle = if (language.id == "he") {
                "עברית (ברירת מחדל)"
            } else {
                stringResource(R.string.trained_on_x_hours, language.trainedHourCount)
            }

            LanguageToggle(language.id, language.name, languages, setLanguages, subtitle)
        }
    }
}
