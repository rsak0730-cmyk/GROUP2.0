package com.example.syncsound

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.concurrent.thread

class AudioCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "AudioServiceChannel")
            .setContentTitle("SyncSound")
            .setContentText("Capturing background audio...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        startForeground(1, notification)

        val resultCode = intent?.getIntExtra("RESULT_CODE", 0) ?: 0
        val dataIntent = intent?.getParcelableExtra<Intent>("DATA_INTENT")

        if (resultCode != 0 && dataIntent != null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, dataIntent)
            startAudioCapture()
        }
        return START_NOT_STICKY
    }

    private fun startAudioCapture() {
        if (mediaProjection == null) return

        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(44100)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        audioRecord?.startRecording()
        isRecording = true

        thread {
            val bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            val audioBuffer = ByteArray(bufferSize)

            while (isRecording) {
                val readBytes = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                if (readBytes > 0) {
                    Log.d("SyncSound", "Captured $readBytes bytes of audio")
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("AudioServiceChannel", "Audio Capture", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        mediaProjection?.stop()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
