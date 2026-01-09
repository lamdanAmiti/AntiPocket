package com.antipocket

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class PocketDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var lightSensor: Sensor? = null
    private lateinit var prefs: PreferencesManager

    private var proximityNear = false
    private var lightLow = false
    private var wasInPocket = false

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager.getInstance(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        registerSensors()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pocket Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors for pocket detection"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Anti Pocket")
            .setContentText("Pocket detection active")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun registerSensors() {
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val maxRange = event.sensor.maximumRange
                proximityNear = event.values[0] < maxRange
            }
            Sensor.TYPE_LIGHT -> {
                // Consider low light as less than 10 lux (typical pocket darkness)
                lightLow = event.values[0] < LIGHT_THRESHOLD
            }
        }

        // Update pocket state
        val currentlyInPocket = proximityNear && lightLow
        isInPocket = currentlyInPocket

        // Re-read preferences to get fresh values
        val antiPocketOn = prefs.antiPocketEnabled
        val lockOn = prefs.lockWhenInPocket

        // Check if phone just entered pocket (anti-pocket feature)
        if (antiPocketOn && !wasInPocket && currentlyInPocket) {
            if (lockOn) {
                // Just lock device, no slider
                lockDevice()
            } else {
                // Show unlock slider only when lock is disabled
                SliderActivity.startForUnlock(this)
            }
        }

        wasInPocket = currentlyInPocket
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    private fun lockDevice() {
        // Use accessibility service to lock - this preserves fingerprint unlock
        CallInterceptorService.lockScreenViaAccessibility()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        isInPocket = false
    }

    companion object {
        private const val CHANNEL_ID = "pocket_detection_channel"
        private const val NOTIFICATION_ID = 1001
        private const val LIGHT_THRESHOLD = 10f // lux

        @Volatile
        var isInPocket = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, PocketDetectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PocketDetectionService::class.java))
        }
    }
}
