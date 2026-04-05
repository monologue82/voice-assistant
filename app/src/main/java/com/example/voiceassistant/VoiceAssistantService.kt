package com.example.voiceassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class VoiceAssistantService : Service(), RecognitionListener {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: Button
    private lateinit var recordingWindow: LinearLayout
    private lateinit var waveformBars: Array<View>
    private lateinit var transcriptTextView: TextView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationManager: NotificationManager
    private var isRecording = false
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var isFloatingButtonDragging = false
    private val okHttpClient = OkHttpClient()

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("voice_assistant", MODE_PRIVATE)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        startForeground(1, createNotification(getString(R.string.idle)))
        setupFloatingButton()
        setupSpeechRecognizer()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingButton()
        removeRecordingWindow()
        speechRecognizer.destroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "voice_assistant_channel",
                "Voice Assistant",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(status: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "voice_assistant_channel")
            .setContentTitle("Voice Assistant")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(status: String) {
        val notification = createNotification(status)
        notificationManager.notify(1, notification)
    }

    private fun setupFloatingButton() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingButton = Button(this)
        floatingButton.text = "🎤"
        floatingButton.textSize = 24f
        floatingButton.setBackgroundResource(android.R.color.transparent)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = sharedPreferences.getInt("floating_button_x", 100)
        params.y = sharedPreferences.getInt("floating_button_y", 300)

        floatingButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isFloatingButtonDragging = false
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(v, params)
                    isFloatingButtonDragging = true
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isFloatingButtonDragging) {
                        startRecording()
                    } else {
                        sharedPreferences.edit()
                            .putInt("floating_button_x", params.x)
                            .putInt("floating_button_y", params.y)
                            .apply()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(floatingButton, params)
    }

    private fun removeFloatingButton() {
        try {
            windowManager.removeView(floatingButton)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(this)
    }

    private fun startRecording() {
        if (!isRecording) {
            isRecording = true
            updateNotification(getString(R.string.recording))
            showRecordingWindow()
            floatingButton.visibility = View.GONE

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, sharedPreferences.getString("language", "zh-CN"))
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            speechRecognizer.startListening(intent)
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            isRecording = false
            speechRecognizer.stopListening()
        }
    }

    private fun showRecordingWindow() {
        recordingWindow = LinearLayout(this)
        recordingWindow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val view = LayoutInflater.from(this).inflate(R.layout.floating_recording_window, recordingWindow)
        waveformBars = arrayOf(
            view.findViewById(R.id.waveform_bar_1),
            view.findViewById(R.id.waveform_bar_2),
            view.findViewById(R.id.waveform_bar_3),
            view.findViewById(R.id.waveform_bar_4),
            view.findViewById(R.id.waveform_bar_5)
        )
        transcriptTextView = view.findViewById(R.id.tv_transcript)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.y = 100

        windowManager.addView(recordingWindow, params)
    }

    private fun removeRecordingWindow() {
        try {
            windowManager.removeView(recordingWindow)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateWaveform(rms: Float) {
        val heights = arrayOf(0.5f, 0.8f, 1.0f, 0.75f, 0.55f)
        for (i in waveformBars.indices) {
            val height = (heights[i] * rms * 0.8f + 0.2f) * 100
            val parentView = waveformBars[i].parent as View
            waveformBars[i].layoutParams = LinearLayout.LayoutParams(
                waveformBars[i].layoutParams.width,
                (height * parentView.height / 100).toInt()
            )
        }
    }

    private fun injectText(text: String) {
        val intent = Intent(this, VoiceAccessibilityService::class.java)
        intent.action = "inject_text"
        intent.putExtra("text", text)
        startService(intent)
    }

    private fun refineTextWithLLM(text: String, callback: (String) -> Unit) {
        val apiBaseUrl = sharedPreferences.getString("api_base_url", "")
        val apiKey = sharedPreferences.getString("api_key", "")
        val model = sharedPreferences.getString("model", "gpt-3.5-turbo")

        if (apiBaseUrl.isNullOrEmpty() || apiKey.isNullOrEmpty() || model.isNullOrEmpty()) {
            callback(text)
            return
        }

        val json = JSONObject()
        json.put("model", model)
        json.put("messages", listOf(
            mapOf(
                "role" to "system",
                "content" to "Only conservative correction: fix obvious speech recognition errors (e.g., Chinese homophones that should be English terms: \"配森\" → \"Python\", \"杰森\" → \"JSON\", \"安桌\" → \"Android\"). Never rewrite, polish, or delete any content that looks correct. If input looks correct, return it as is."
            ),
            mapOf(
                "role" to "user",
                "content" to text
            )
        ))

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$apiBaseUrl/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post { callback(text) }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody)
                    val refinedText = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    Handler(Looper.getMainLooper()).post { callback(refinedText) }
                } else {
                    Handler(Looper.getMainLooper()).post { callback(text) }
                }
            }
        })
    }

    override fun onReadyForSpeech(params: Bundle?) {
        transcriptTextView.text = getString(R.string.recording)
    }

    override fun onBeginningOfSpeech() {
    }

    override fun onRmsChanged(rmsdB: Float) {
        updateWaveform(rmsdB)
    }

    override fun onBufferReceived(buffer: ByteArray?) {
    }

    override fun onEndOfSpeech() {
        updateNotification(getString(R.string.refining))
        transcriptTextView.text = getString(R.string.refining)
    }

    override fun onError(error: Int) {
        isRecording = false
        floatingButton.visibility = View.VISIBLE
        removeRecordingWindow()
        updateNotification(getString(R.string.idle))
        Toast.makeText(this, "Speech recognition error: $error", Toast.LENGTH_SHORT).show()
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            if (sharedPreferences.getBoolean("llm_refinement_enabled", false)) {
                refineTextWithLLM(text) { refinedText ->
                    injectText(refinedText)
                    floatingButton.visibility = View.VISIBLE
                    removeRecordingWindow()
                    updateNotification(getString(R.string.idle))
                }
            } else {
                injectText(text)
                floatingButton.visibility = View.VISIBLE
                removeRecordingWindow()
                updateNotification(getString(R.string.idle))
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            transcriptTextView.text = matches[0]
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
    }
}