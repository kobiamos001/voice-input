package org.futo.voiceinput.settings.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.futo.voiceinput.R
import org.futo.voiceinput.settings.ScreenTitle
import org.futo.voiceinput.settings.ScrollableList
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

    ScrollableList {
        ScreenTitle(title = stringResource(id = R.string.help_title), showBack = true, navController = navController)

        // 1. מדריך שימוש חדש ומפורט לעוזר הקולי וללחצן הצף
        HelpBubble(title = stringResource(id = R.string.help_assistant_guide_title)) {
            Column {
                textItem(stringResource(id = R.string.help_assistant_guide_desc))
                Spacer(modifier = Modifier.height(8.dp))
                textItem(stringResource(id = R.string.help_assistant_guide_step_1))
                textItem(stringResource(id = R.string.help_assistant_guide_step_2))
                textItem(stringResource(id = R.string.help_assistant_guide_step_3))
                textItem(stringResource(id = R.string.help_assistant_guide_step_4))
            }
        }

        // 2. הסבר כללי על המקלדת והקלדה קולית
        HelpBubble(title = stringResource(id = R.string.help_keyboard_typing_title)) {
            Column {
                textItem(stringResource(R.string.help_paragraph_1))
                textItem(stringResource(R.string.help_paragraph_2))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
