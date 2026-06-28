package org.futo.voiceinput.settings.pages

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.futo.voiceinput.R
import org.futo.voiceinput.RecognizeWindow
import org.futo.voiceinput.settings.NavigationItem
import org.futo.voiceinput.settings.NavigationItemStyle
import org.futo.voiceinput.settings.ScreenTitle
import org.futo.voiceinput.settings.ScrollableList
import org.futo.voiceinput.settings.Tip
import org.futo.voiceinput.settings.openImeOptions
import org.futo.voiceinput.theme.Typography


// קומפוננטת כרטיס בועה מודרנית לשמירה על אחידות העיצוב
@Composable
fun HelpBubble(
    title: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            content()
        }
    }
}

@Composable
@Preview
fun HelpScreen(navController: NavHostController = rememberNavController()) {
    val textItem: @Composable (text: String) -> Unit = { text ->
        Text(text, style = Typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
    }

    val context = LocalContext.current
    val antiConfusionText = stringResource(R.string.just_a_demonstration)
    val showAntiConfusionToast = {
        val toast = Toast.makeText(
            context,
            antiConfusionText,
            Toast.LENGTH_SHORT
        )
        toast.show()
    }

    ScrollableList {
        ScreenTitle(title = stringResource(id = R.string.help_title), showBack = true, navController = navController)

        // 1. מדריך שימוש חדש ומפורט לעוזר הקולי וללחצן הצף
        HelpBubble(title = "מדריך לשימוש בעוזר הקולי (לחצן צף)") {
            Column {
                textItem("העוזר הקולי מאפשר לך להקליד ולבצע פקודות מכל מקום במכשיר באמצעות לחצן צף ונוח:")
                Spacer(modifier = Modifier.height(8.dp))
                textItem("• הפעלה מהירה: הקש פעם אחת על לחצן המיקרופון הירוק כדי להתחיל לדבר.")
                textItem("• זיהוי מצבים: הלחצן משנה את צבעו בהתאם למצב (ירוק להמתנה, אדום להאזנה, וכתום בזמן עיבוד וכתיבה).")
                textItem("• גרירה ומיקום: ניתן לגרור את הלחצן הצף לכל מקום שנוח לך על גבי המסך.")
            }
        }

        // 2. הסבר כללי על המקלדת והקלדה קולית
        HelpBubble(title = "הקלדה קולית במקלדת") {
            Column {
                textItem(stringResource(R.string.help_paragraph_1))
                textItem(stringResource(R.string.help_paragraph_2))
            }
        }

        // 3. תצוגת הדגמת חלון זיהוי קולי 1
        HelpBubble(title = "איך נראה חלון ההקלטה?") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.align(CenterHorizontally)) {
                    RecognizeWindow(onClose = showAntiConfusionToast, forceNoUnpaidNotice = true) {
                        Text(
                            stringResource(R.string.voice_input_will_look_like_this),
                            modifier = Modifier.align(CenterHorizontally),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            stringResource(R.string.look_for_the_big_off_center_futo_logo_in_the_background),
                            style = Typography.bodyMedium,
                            modifier = Modifier
                                .padding(2.dp, 4.dp)
                                .align(CenterHorizontally),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 4. תצוגת הדגמת חלון זיהוי קולי 2 (עצירה)
        HelpBubble(title = "סיום הקלטה") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.align(CenterHorizontally)) {
                    RecognizeWindow(onClose = showAntiConfusionToast, forceNoUnpaidNotice = true) {
                        IconButton(
                            onClick = showAntiConfusionToast,
                            modifier = Modifier.align(CenterHorizontally)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.mic_2_),
                                contentDescription = stringResource(R.string.stop_recording),
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.once_you_re_done_talking_you_can_hit_the_microphone_button_to_stop),
                            style = Typography.bodyMedium,
                            modifier = Modifier
                                .padding(2.dp, 4.dp)
                                .align(CenterHorizontally),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 5. מקלדות תואמות (מקטע 1)
        HelpBubble(title = "מקלדות תואמות מומלצות") {
            Column {
                textItem(stringResource(R.string.help_paragraph_3))
                Spacer(modifier = Modifier.height(8.dp))
                textItem(stringResource(R.string.aosp_keyboard_included_in_aosp_based_roms))
                textItem(stringResource(R.string.heliboard_info))
                textItem(stringResource(R.string.anysoftkeyboard_available_on_f_droid_and_google_play))
                textItem(stringResource(R.string.unexpected_keyboard))
            }
        }

        // 6. מקלדות לא תואמות לחלוטין (מקטע 2)
        HelpBubble(title = "מקלדות לא תואמות באופן חלקי") {
            Column {
                textItem(stringResource(R.string.help_paragraph_5))
                Spacer(modifier = Modifier.height(8.dp))
                textItem(stringResource(R.string.help_paragraph_6))
                Spacer(modifier = Modifier.height(4.dp))
                textItem(stringResource(R.string.grammarly_keyboard))
                textItem(stringResource(R.string.microsoft_swiftkey))
            }
        }

        // 7. מקלדות לא תואמות (מקטע 3)
        HelpBubble(title = "מקלדות שאינן נתמכות") {
            Column {
                textItem(stringResource(R.string.help_paragraph_7))
                Spacer(modifier = Modifier.height(8.dp))
                textItem(stringResource(R.string.help_paragraph_8))
                Spacer(modifier = Modifier.height(4.dp))
                textItem(stringResource(R.string.gboard))
                textItem(stringResource(R.string.samsung_keyboard_one_ui_5))
                textItem(stringResource(R.string.typewise))
                textItem(stringResource(R.string.simple_keyboard))
            }
        }

        // 8. פתרון בעיות והגדרת ברירת מחדל
        HelpBubble(title = "שינוי הגדרות קלט קולי ברירת מחדל") {
            Column {
                textItem(stringResource(R.string.help_paragraph_9))
                Spacer(modifier = Modifier.height(8.dp))
                textItem(stringResource(R.string.wrong_voice_input_body))
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { openImeOptions(context) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(stringResource(R.string.open_input_method_settings))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
