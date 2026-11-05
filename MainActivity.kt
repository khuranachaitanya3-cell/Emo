package com.chaitanya.emobuddy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var face: TextView
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var mic: Button
    private lateinit var endpoint: EditText
    private lateinit var tts: TextToSpeech
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var handsFree = false
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 42)
        }
        if (SpeechRecognizer.isRecognitionAvailable(this)) recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { runOnUiThread { mood("👀", "Listening…") } }
            override fun onBeginningOfSpeech() { runOnUiThread { mood("😮", "I’m listening!") } }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { runOnUiThread { mood("🤔", "Thinking…") } }
            override fun onError(error: Int) {
                runOnUiThread {
                    listening = false
                    if (handsFree) startListening() else mood("🙂", "Tap the mic to talk")
                }
            }
            override fun onResults(results: Bundle?) {
                val said = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                runOnUiThread { listening = false; if (said.isNotBlank()) { transcript.text = "You: $said"; mood("🤔", "Thinking…"); askBuddy(said) } }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val said = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!said.isNullOrBlank()) runOnUiThread { transcript.text = "You: $said" }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun buildUi() {
        val bg = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(8,18,42), Color.rgb(23,40,78)))
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(24, 28, 24, 24); background = bg
        }
        fun label(text: String, size: Float, color: Int = Color.WHITE): TextView =
            TextView(this).apply { this.text = text; textSize = size; setTextColor(color); gravity = Gravity.CENTER }
        layout.addView(label("EMOBUDDY", 15f, Color.rgb(110,210,255)).apply { letterSpacing = .25f })
        face = label("😊", 104f).apply { setPadding(0, 24, 0, 8) }
        layout.addView(face)
        status = label("Your little AI pal", 18f)
        layout.addView(status)
        val sub = label("Talk naturally • Watch my expressions change", 13f, Color.LTGRAY)
        sub.setPadding(0, 8, 0, 24); layout.addView(sub)
        transcript = label("Hey! Tap the mic and say hello 👋", 16f)
        transcript.setPadding(12, 18, 12, 18)
        transcript.background = rounded(Color.argb(45,255,255,255), 22)
        layout.addView(transcript, LinearLayout.LayoutParams(-1, 110))
        mic = Button(this).apply { text = "🎙  START TALKING"; textSize = 16f; isAllCaps = false; setOnClickListener { toggleListening() } }
        val micLp = LinearLayout.LayoutParams(-1, 58); micLp.setMargins(0, 22, 0, 8); layout.addView(mic, micLp)
        val hands = CheckBox(this).apply {
            text = "Hands-free conversation (listen again after replies)"
            setTextColor(Color.WHITE); buttonTintList = android.content.res.ColorStateList.valueOf(Color.CYAN)
            setOnCheckedChangeListener { _, checked -> handsFree = checked }
        }
        layout.addView(hands)
        endpoint = EditText(this).apply {
            hint = "AI server URL (e.g. http://192.168.1.5:8000/chat)"
            setSingleLine(true); textSize = 12f; setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY)
            setPadding(12, 8, 12, 8); background = rounded(Color.argb(35,255,255,255), 14)
        }
        val epLp = LinearLayout.LayoutParams(-1, 48); epLp.setMargins(0, 12, 0, 0); layout.addView(endpoint, epLp)
        layout.addView(label("Connect a private AI backend for smart replies.", 11f, Color.LTGRAY).apply { setPadding(0, 8, 0, 0) })
        setContentView(layout)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = radius.toFloat() }

    private fun mood(emoji: String, message: String) {
        face.text = emoji
        status.text = message
    }

    private fun toggleListening() {
        if (listening) {
            listening = false; handsFree = false
            recognizer?.cancel(); mic.text = "🎙  START TALKING"; mood("🙂", "Paused")
        } else startListening()
    }

    private fun startListening() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 42); return
        }
        if (recognizer == null) { mood("😕", "Speech recognition is unavailable"); return }
        try {
            listening = true; mic.text = "■  STOP"
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer?.startListening(intent)
        } catch (_: Exception) { listening = false; mood("😕", "Could not start microphone") }
    }

    private fun askBuddy(message: String) {
        val api = endpoint.text.toString().trim()
        if (api.isBlank()) {
            val reply = "I heard you say: $message. Connect your AI server URL below to give me a real AI brain!"
            speak(reply, "🙂"); return
        }
        thread {
            var answer = "Oops! I couldn't reach my AI server. Please check the server URL and Wi-Fi."
            try {
                val conn = (URL(api).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 12000; readTimeout = 20000; doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write(JSONObject().put("message", message).toString().toByteArray()) }
                val body = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream).bufferedReader().use { it.readText() }
                answer = JSONObject(body).optString("reply", answer)
                conn.disconnect()
            } catch (_: Exception) {}
            runOnUiThread { speak(answer, "😄") }
        }
    }

    private fun speak(text: String, expression: String) {
        transcript.text = "EmoBuddy: $text"
        mood(expression, "Speaking…")
        if (ready) {
            tts.setPitch(1.25f); tts.setSpeechRate(0.92f)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "emobuddy_reply")
        }
        if (handsFree) {
            // Resume after speech finishes; fallback delay is avoided by TTS listener.
            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { runOnUiThread { if (handsFree) startListening() else mood("😊", "Ready for another chat") } }
                @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { runOnUiThread { mood("😕", "Voice playback error") } }
            })
        } else mood(expression, "Ready for another chat")
    }

    override fun onInit(statusCode: Int) {
        ready = statusCode == TextToSpeech.SUCCESS
        if (ready) tts.language = Locale.getDefault()
    }

    override fun onDestroy() {
        recognizer?.destroy(); tts.stop(); tts.shutdown(); super.onDestroy()
    }
}
