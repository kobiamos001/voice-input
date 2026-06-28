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

        val trimmedResult = result.trim()
        if (context is FloatingAssistantService) {
            // ביצוע הפקודה באופן מיידי וללא שיהוי
            CommandParser.parseAndExecute(context, trimmedResult) { statusMessage ->
                (context as FloatingAssistantService).updateLiveStatus(statusMessage)
            }
        } else {
            CommandParser.parseAndExecute(context, result) {}
        }
        
        reset()
        onStateChanged(State.IDLE)
        // הוסרה ההפעלה מחדש האוטומטית שהייתה קיימת במצב חכם
    }

    override fun cancelled() {
        reset()
        onStateChanged(State.IDLE)
        // הוסרה ההפעלה מחדש האוטומטית שהייתה קיימת במצב חכם
    }

    override fun languageDetected(result: String) {}
    override fun partialResult(result: String) {}
    override fun decodingStatus(status: RunState) {}
}
