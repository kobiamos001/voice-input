package org.futo.voiceinput

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope

class FloatingAssistantService : Service(), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private lateinit var windowManager: WindowManager
    private var containerView: LinearLayout? = null
    private var floatingButton: ImageView? = null
    private var statusTextView: TextView? = null
    private var recognizer: AssistantRecognizer? = null
    
    private val handler = Handler(Looper.getMainLooper())

    private val NOTIFICATION_ID = 888
    private val CHANNEL_ID = "FloatingAssistantChannel"

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true 
        
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Assistant:WakeLock")
            wakeLock?.acquire()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        startServiceForeground()

        recognizer = AssistantRecognizer(this, this.lifecycleScope) { state ->
            updateUIByState(state)
        }

        val prefs = getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)
        val assistantType = prefs.getString("assistant_type", "floating") ?: "floating"
        val isContinuous = prefs.getBoolean("continuous_listening", false)

        if (assistantType == "floating") {
            setupFloatingWidget()
        }

        // אם האזנה רציפה מופעלת, נתחיל להאזין באופן מיידי כבר באתחול השירות
        if (isContinuous) {
            recognizer?.create()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                "START_RECORDING" -> {
                    recognizer?.let {
                        if (!it.isCurrentlyRecording()) {
                            it.create()
                        }
                    }
                }
                "STOP_RECORDING" -> {
                    recognizer?.let {
                        if (it.isCurrentlyRecording()) {
                            it.finishRecognizerIfRecording()
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun startServiceForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "שירות עוזר קולי",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val title = "העוזר הקולי פעיל"
        val desc = "לחצן המיקרופון הצף זמין על המסך"

        val notification = buildNotification(title, desc)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, desc: String) {
        try {
            val notification = buildNotification(title, desc)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(title: String, desc: String): Notification {
        val prefs = getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)
        val assistantType = prefs.getString("assistant_type", "floating") ?: "floating"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(desc)
            .setSmallIcon(R.drawable.ic_keyboard_voice)
            .setOnlyAlertOnce(true)

        if (assistantType == "notification") {
            val startIntent = Intent(this, FloatingAssistantService::class.java).apply {
                action = "START_RECORDING"
            }
            val startPendingIntent = PendingIntent.getService(
                this, 1, startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            builder.addAction(R.drawable.ic_keyboard_voice, "הפעל האזנה", startPendingIntent)

            val stopIntent = Intent(this, FloatingAssistantService::class.java).apply {
                action = "STOP_RECORDING"
            }
            val stopPendingIntent = PendingIntent.getService(
                this, 2, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            builder.addAction(R.drawable.ic_keyboard_voice, "עצור האזנה", stopPendingIntent)
        }

        return builder.build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingWidget() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        containerView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 36f
                // הוגברה השקיפות של הרקע השחור מ-CC ל-77 (כ-45% שקיפות)
                setColor(Color.parseColor("#77222222"))
            }
            setPadding(10, 6, 20, 6)
        }

        val buttonSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 36f, resources.displayMetrics
        ).toInt()
        val buttonParams = LinearLayout.LayoutParams(buttonSize, buttonSize)

        floatingButton = ImageView(this).apply {
            layoutParams = buttonParams
            setImageResource(R.drawable.ic_keyboard_voice)
            setColorFilter(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4CAF50"))
            }
            setPadding(8, 8, 8, 8)
        }

        statusTextView = TextView(this).apply {
            text = ""
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(14, 0, 8, 0)
            visibility = View.GONE
        }

        containerView!!.addView(floatingButton)
        containerView!!.addView(statusTextView)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        containerView!!.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private var isDragging: Boolean = false
            private val TOUCH_SLOP = 10 

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()

                        if (Math.abs(deltaX) > TOUCH_SLOP || Math.abs(deltaY) > TOUCH_SLOP) {
                            isDragging = true
                            params.x = initialX + deltaX
                            params.y = initialY + deltaY
                            windowManager.updateViewLayout(containerView, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            toggleAssistant()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(containerView, params)
    }

    private fun toggleAssistant() {
        recognizer?.let {
            if (it.isCurrentlyRecording()) {
                it.finishRecognizerIfRecording()
            } else {
                it.create()
            }
        }
    }

    private fun updateUIByState(state: AssistantRecognizer.State) {
        val button = floatingButton ?: return
        val text = statusTextView ?: return

        when (state) {
            AssistantRecognizer.State.IDLE -> {
                button.setImageResource(R.drawable.ic_keyboard_voice)
                button.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#4CAF50"))
                }
                
                if (text.text == "מעבד..." || text.text == "מאזין...") {
                    text.visibility = View.GONE
                } else {
                    handler.postDelayed({
                        text.visibility = View.GONE
                    }, 3000)
                }

                updateNotification("העוזר הקולי פעיל", "שירות העוזר הקולי פעיל וממתין לפקודה")
            }
            AssistantRecognizer.State.RECORDING -> {
                text.visibility = View.VISIBLE
                text.text = "מאזין..."
                text.setTextColor(Color.parseColor("#FF5252"))
                button.setImageResource(R.drawable.ic_keyboard_voice)
                button.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#FF5252"))
                }
                updateNotification("עוזר קולי: מאזין", "מאזין לדיבור שלך...")
            }
            AssistantRecognizer.State.PROCESSING -> {
                text.visibility = View.VISIBLE
                text.text = "מעבד..."
                text.setTextColor(Color.parseColor("#FFC107"))
                button.setImageResource(R.drawable.ic_keyboard_voice)
                button.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#FFC107"))
                }
                updateNotification("עוזר קולי: מעבד", "מפענח ומנתח את הפקודה...")
            }
            AssistantRecognizer.State.FINISHED -> {
                // המצב הצהוב (PROCESSING) נשאר פעיל עד לביצוע הפעולה בפועל
            }
        }
    }

    fun updateLiveStatus(message: String) {
        floatingButton?.setImageResource(R.drawable.ic_keyboard_voice)
        floatingButton?.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#4CAF50"))
        }
        
        statusTextView?.let { text ->
            text.visibility = View.VISIBLE
            text.text = message
            text.setTextColor(Color.parseColor("#4CAF50"))
        }

        updateNotification("העוזר הקולי פעיל", message)

        handler.postDelayed({
            statusTextView?.visibility = View.GONE
        }, 3000)
    }

    override fun onDestroy() {
        isRunning = false 
        
        handler.removeCallbacksAndMessages(null)
        recognizer?.reset()
        if (containerView != null && containerView!!.parent != null) {
            windowManager.removeView(containerView)
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
