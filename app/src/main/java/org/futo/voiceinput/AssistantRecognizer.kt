package org.futo.voiceinput

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.futo.voiceinput.ml.RunState

class AssistantRecognizer(
    private val mContext: Context,
    private val mScope: LifecycleCoroutineScope,
    private val onStateChanged: (State) -> Unit
) : AudioRecognizer() {

    init {
        forceLanguage("he")
    }

    enum class State { IDLE, RECORDING, PROCESSING, FINISHED }

    override val context: Context get() = mContext
    override val lifecycleScope: LifecycleCoroutineScope get() = mScope

    override fun loading() { onStateChanged(State.PROCESSING) }
    override fun needPermission() {}
    override fun permissionRejected() {}
    override fun recordingStarted() { onStateChanged(State.RECORDING) }
    override fun updateMagnitude(magnitude: Float, state: MagnitudeState) {}
    override fun processing() { onStateChanged(State.PROCESSING) }

    override fun finished(result: String) {
        onStateChanged(State.FINISHED)

        if (context is FloatingAssistantService) {
            // הצגת הטקסט שפוענח בפועל בשורת המשימות לצורך בדיקה ודיבאג
            val trimmedResult = result.trim()
            (context as FloatingAssistantService).updateLiveStatus("זיהה: \"$trimmedResult\"")
            
            // השהייה קלה של 1.5 שניות כדי שהמשתמש יספיק לקרוא את הטקסט שפוענח לפני ביצוע הפקודה
            lifecycleScope.launch {
                delay(1500L)
                CommandParser.parseAndExecute(context, trimmedResult) { statusMessage ->
                    (context as FloatingAssistantService).updateLiveStatus(statusMessage)
                }
            }
        } else {
            CommandParser.parseAndExecute(context, result) {}
        }
        
        reset()
        onStateChanged(State.IDLE)

        // הפעלה מחדש במצב חכם - עם השהיית חומרה בטוחה של 350ms לשחרור המיקרופון
        val prefs = context.getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)
        val isSmartMode = prefs.getBoolean("smart_assistant_mode", false)
        if (isSmartMode && FloatingAssistantService.isRunning) {
            lifecycleScope.launch {
                delay(350L) 
                create()
            }
        }
    }

    override fun cancelled() {
        reset()
        onStateChanged(State.IDLE)

        val prefs = context.getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)
        val isSmartMode = prefs.getBoolean("smart_assistant_mode", false)
        if (isSmartMode && FloatingAssistantService.isRunning) {
            lifecycleScope.launch {
                delay(350L) 
                create()
            }
        }
    }

    override fun languageDetected(result: String) {}
    override fun partialResult(result: String) {}
    override fun decodingStatus(status: RunState) {}
}
