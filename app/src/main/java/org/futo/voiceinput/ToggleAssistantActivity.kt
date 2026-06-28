package org.futo.voiceinput

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.futo.voiceinput.theme.UixThemeAuto

class ToggleAssistantActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UixThemeAuto {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var hasOverlayPermission by remember {
                        mutableStateOf(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                Settings.canDrawOverlays(this)
                            } else {
                                true
                            }
                        )
                    }

                    DisposableEffect(Unit) {
                        onDispose {}
                    }

                    if (!hasOverlayPermission) {
                        PermissionExplanationScreen(
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName")
                                    )
                                    startActivity(intent)
                                }
                            }
                        )
                    } else {
                        AssistantControlScreen(
                            onToggle = { enable ->
                                if (enable) {
                                    startService(Intent(this, FloatingAssistantService::class.java))
                                } else {
                                    stopService(Intent(this, FloatingAssistantService::class.java))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setContent {
            UixThemeAuto {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Settings.canDrawOverlays(this)
                    } else {
                        true
                    }

                    if (!hasOverlayPermission) {
                        PermissionExplanationScreen(
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName")
                                    )
                                    startActivity(intent)
                                }
                            }
                        )
                    } else {
                        AssistantControlScreen(
                            onToggle = { enable ->
                                if (enable) {
                                    startService(Intent(this, FloatingAssistantService::class.java))
                                } else {
                                    stopService(Intent(this, FloatingAssistantService::class.java))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionExplanationScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "נדרשת הרשאת חלון צף",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "כדי שנוכל להציג את לחצן המיקרופון הצף מעל אפליקציות אחרות, יש לאשר את ההרשאה 'הצגה מעל אפליקציות אחרות' בהגדרות המערכת.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRequestPermission) {
            Text("מעבר להגדרות המערכת לאישור")
        }
    }
}

@Composable
fun AssistantControlScreen(onToggle: (Boolean) -> Unit) {
    var isServiceActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "עוזר קולי אופליין",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "לחץ על הכפתור למטה כדי להפעיל או לכבות את לחצן העוזר הקולי הצף.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                isServiceActive = !isServiceActive
                onToggle(isServiceActive)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isServiceActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = if (isServiceActive) "כבה לחצן צף" else "הפעל לחצן צף")
        }
    }
}
