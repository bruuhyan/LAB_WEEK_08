package com.example.lab_week_08

import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NotificationService : Service() {

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // Buat notifikasi & jalankan foreground service
        notificationBuilder = startForegroundNotification()

        // Siapkan HandlerThread untuk menjalankan proses di thread terpisah
        val handlerThread = HandlerThread("SecondThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    /**
     * Membuat notifikasi dan memulai foreground service.
     */
    private fun startForegroundNotification(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()

        val builder = getNotificationBuilder(pendingIntent, channelId)

        startForeground(NOTIFICATION_ID, builder.build())
        return builder
    }

    /**
     * PendingIntent: aksi saat user klik notifikasi → buka MainActivity
     */
    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0

        return PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            flag
        )
    }

    /**
     * Membuat notification channel (dibutuhkan mulai Android O / API 26)
     */
    private fun createNotificationChannel(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "001"
            val channelName = "001 Channel"
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(channelId, channelName, channelPriority)

            val notificationManager = ContextCompat.getSystemService(
                this,
                NotificationManager::class.java
            ) ?: return channelId

            notificationManager.createNotificationChannel(channel)
            channelId
        } else {
            "" // channel tidak diperlukan di Android < 8.0
        }
    }

    /**
     * Membuat NotificationCompat.Builder dengan konfigurasi lengkap.
     */
    private fun getNotificationBuilder(
        pendingIntent: PendingIntent,
        channelId: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Second worker process is done")
            .setContentText("Check it out!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Second worker process is done, check it out!")
            .setOngoing(true) // notifikasi tidak bisa dihapus manual
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)

        // Gets the channel id passed from the MainActivity through the Intent
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        // Jalankan pekerjaan di thread terpisah
        serviceHandler.post {
            // Hitung mundur dari 10 ke 0 dan update notifikasi
            countDownFromTenToZero(notificationBuilder)

            // Notifikasi ke MainActivity bahwa pekerjaan sudah selesai
            notifyCompletion(Id)

            // Hentikan foreground (hapus notifikasi)
            stopForeground(STOP_FOREGROUND_REMOVE)

            // Hentikan service
            stopSelf()
        }

        return returnValue
    }

    /**
     * Update notifikasi setiap detik untuk menampilkan hitung mundur.
     */
    private fun countDownFromTenToZero(notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        for (i in 10 downTo 0) {
            Thread.sleep(1000L)

            notificationBuilder.setContentText("$i seconds until last warning")
                .setSilent(true)

            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    /**
     * Update LiveData di Main Thread setelah countdown selesai.
     */
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"

        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
