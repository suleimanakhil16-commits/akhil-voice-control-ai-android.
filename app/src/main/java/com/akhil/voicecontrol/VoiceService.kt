
package com.akhil.voicecontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var notificationManager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private val channelId = "akhil_voice_channel"
    private val notificationId = 1

    override fun onCreate() {
        super.onCreate()

        notificationManager =
            getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            channelId,
            "Akhil Voice Control",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        startForeground(
            notificationId,
            createNotification("Starting speech recognition..."),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        startSpeechRecognition()
    }

    private fun createNotification(message: String): Notification {
        return Notification.Builder(this, channelId)
            .setContentTitle("AKHIL VOICE CONTROL AI")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(message: String) {
        notificationManager.notify(
            notificationId,
            createNotification(message)
        )
    }

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateNotification("Speech recognition is unavailable")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    updateNotification("Listening for your voice...")
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    updateNotification("Processing your speech...")
                }

                override fun onError(error: Int) {
                    updateNotification("Listening paused. Restarting...")
                    handler.postDelayed({
                        startListeningAgain()
                    }, 1200)
                }

                override fun onResults(results: Bundle?) {
                    val words = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    val spokenText = words?.firstOrNull()

                    updateNotification(
                        if (spokenText.isNullOrBlank()) {
                            "Listening for your voice..."
                        } else {
                            "Heard: $spokenText"
                        }
                    )

                    handler.postDelayed({
                        startListeningAgain()
                    }, 700)
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
        )

        startListeningAgain()
    }

    private fun startListeningAgain() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-NG")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            updateNotification("Could not start listening")
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
