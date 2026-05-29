package com.minlish.app.ui.learn

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.minlish.app.data.model.Word
import java.util.Locale

class WordSpeaker(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var mediaPlayer: MediaPlayer? = null
    private var pendingWord: Word? = null
    private var pendingOnDone: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            tts?.language = Locale.US
            val word = pendingWord
            val onDone = pendingOnDone
            pendingWord = null
            pendingOnDone = null
            if (word != null) {
                speakWord(word, onDone ?: {})
            }
        }
    }

    fun speakWord(word: Word, onDone: () -> Unit = {}) {
        val audioUrl = word.audioUrl?.trim()
        if (!audioUrl.isNullOrEmpty()) {
            playUrl(audioUrl, word.word, onDone)
            return
        }
        if (!ready) {
            pendingWord = word
            pendingOnDone = onDone
            return
        }
        speakText(word.word, onDone)
    }

    private fun speakText(text: String, onDone: () -> Unit) {
        if (!ready) {
            onDone()
            return
        }
        val utteranceId = "word_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                onDone()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onDone()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                onDone()
            }
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
            onDone()
        }
    }

    private fun playUrl(url: String, fallbackText: String, onDone: () -> Unit) {
        releaseMediaPlayer()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    releaseMediaPlayer()
                    onDone()
                }
                setOnErrorListener { _, _, _ ->
                    releaseMediaPlayer()
                    speakText(fallbackText, onDone)
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            releaseMediaPlayer()
            speakText(fallbackText, onDone)
        }
    }

    fun stop() {
        pendingWord = null
        pendingOnDone = null
        tts?.stop()
        releaseMediaPlayer()
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
