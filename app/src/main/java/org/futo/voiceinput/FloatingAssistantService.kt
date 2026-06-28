package org.futo.voiceinput

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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

        // שמירה על ערנות המעבד גם כאשר המסך כבוי לצורך האזנה רציפה ברקע
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

        // בדיקה האם מצב עוזר חכם פעיל
        val prefs = getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)
        val isSmartMode = prefs.getBoolean("smart_assistant_mode", false)

        if (isSmartMode) {
            // במצב חכם אין צורך בלחצן צף, אנו מתחילים להאזין מיד ברקע
            recognizer?.create()
        } else {
            // במצב רגיל אנו בונים את הלחצן הצף ומחכים ללחיצה
            setupFloatingWidget()
        }
    }

    private fun startServiceForeground() {
        val prefs = getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)
        val isSmartMode = prefs.getBoolean("smart_assistant_mode", false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "שירות עוזר קולי",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // התאמת תוכן ההתראה לפי המצב הפעיל
        val title = if (isSmartMode) "מצב עוזר חכם פעיל" else "העוזר הקולי פעיל"
        val desc = if (isSmartMode) "האזנה רציפה ברקע באנרגיה נמוכה פעילה" else "לחצן המיקרופון הצף זמין על המסך"

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(desc)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingWidget() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        containerView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 75f
                setColor(Color.parseColor("#CC222222"))
            }
            setPadding(15, 10, 30, 10)
        }

        floatingButton = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4CAF50"))
            }
            setPadding(25, 25, 25, 25)
        }

        statusTextView = TextView(this).apply {
            text = ""
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(20, 0, 10, 0)
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

        handler.removeCallbacksAndMessages(null)

        when (state) {
            AssistantRecognizer.State.IDLE -> {
                button.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#4CAF50"))
                }
                handler.postDelayed({
                    text.visibility = View.GONE
                }, 3000)
            }
            AssistantRecognizer.State.RECORDING -> {
                text.visibility = View.VISIBLE
                text.text = "מאזין... 🎙️"
                text.setTextColor(Color.parseColor("#FF5252"))
                button.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#FF5252"))
                }
            }
            AssistantRecognizer.State.PROCESSING -> {
                text.visibility = View.VISIBLE
                text.text = "מעבד... ⚙️"
                text.setTextColor(Color.parseColor("#FFC107"))
                button.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#FFC107"))
                }
            }
            AssistantRecognizer.State.FINISHED -> {
                button.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#4CAF50"))
                }
            }
        }
    }

    fun updateLiveStatus(message: String) {
        statusTextView?.let { text ->
            text.visibility = View.VISIBLE
            text.text = message
            text.setTextColor(Color.parseColor("#4CAF50"))
        }
    }

    override fun onDestroy() {
        isRunning = false 
        
        handler.removeCallbacksAndMessages(null)
        recognizer?.reset()
        if (containerView != null) {
            windowManager.removeView(containerView)
        }

        // שחרור ה-WakeLock בצורה מאובטחת בעת סגירת השירות
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
