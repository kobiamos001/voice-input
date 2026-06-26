package org.futo.voiceinput.settings.pages

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
    val disabled = true // חסימת האפשרות לבטל את השפה היחידה שנשארה

    SettingToggleRaw(
        name,
        languages.contains(id),
        {
            setLanguages(setOf("he"))
        },
        subtitle = subtitle,
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
        if (!multilingual) {
            setMultilingual(true)
        }
        context.startModelDownloadActivity(listOf(MULTILINGUAL_MODELS[multilingualModelIndex]))
    }

    LaunchedEffect(languages) {
        // כפייה מוחלטת של השפה העברית בלבד במסד הנתונים
        if (languages.size != 1 || !languages.contains("he")) {
            setLanguages(setOf("he"))
        }
        if (!multilingual) setMultilingual(true)
    }

    // סינון הרשימה כך שתכיל עברית בלבד
    val filteredLanguages = LANGUAGE_LIST.filter { it.id == "he" }

    SettingListLazy {
        item {
            ScreenTitle(stringResource(R.string.languages_title), showBack = true, navController = navController)
        }

        item {
            Tip("ההקלדה הקולית מוגדרת כעת לזהות עברית בלבד באופן קבוע.")
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(filteredLanguages.size) {
            val language = filteredLanguages[it]
            LanguageToggle(language.id, language.name, languages, setLanguages, "עברית (פעילה תמיד)")
        }
    }
}