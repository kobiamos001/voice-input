package org.futo.voiceinput

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import org.futo.voiceinput.ml.RunState

class AssistantRecognizer(
    private val mContext: Context,
    private val mScope: LifecycleCoroutineScope,
    private val onStateChanged: (State) -> Unit
) : AudioRecognizer() {

    // כפיית שפה עברית קבועה עבור מנוע העוזר הקולי
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

        // שליחת הטקסט של הפעולה לעדכון חי על גבי הקפסולה
        if (context is FloatingAssistantService) {
            CommandParser.parseAndExecute(context, result) { statusMessage ->
                (context as FloatingAssistantService).updateLiveStatus(statusMessage)
            }
        } else {
            CommandParser.parseAndExecute(context, result) {}
        }
        
        reset()
        onStateChanged(State.IDLE)
    }

    override fun cancelled() {
        reset()
        onStateChanged(State.IDLE)
    }

    override fun languageDetected(result: String) {}
    override fun partialResult(result: String) {}
    override fun decodingStatus(status: RunState) {}
}
