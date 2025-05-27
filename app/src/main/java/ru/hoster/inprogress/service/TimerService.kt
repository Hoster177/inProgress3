package ru.hoster.inprogress.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import ru.hoster.inprogress.MainActivity
import ru.hoster.inprogress.R
import ru.hoster.inprogress.data.ActivityItem
import ru.hoster.inprogress.domain.model.ActivityRepository
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timerJob: Job? = null

    // текущий ID задачи (firebaseId или локальный id-toString)
    private var currentTaskId: String? = null
    // время старта этого запуска
    private var taskStartTimeMillis: Long = 0L
    // уже накопленное до этого время
    private var accumulatedTimeMillis: Long = 0L

    @Inject lateinit var activityRepository: ActivityRepository

    companion object {
        const val ACTION_START_TIMER = "ACTION_START_TIMER"
        const val ACTION_STOP_TIMER  = "ACTION_STOP_TIMER"
        const val EXTRA_TASK_ID      = "EXTRA_TASK_ID"
        const val EXTRA_TASK_NAME    = "EXTRA_TASK_NAME"
        const val EXTRA_ACCUMULATED_TIME = "EXTRA_ACCUMULATED_TIME"

        private const val NOTIF_CHANNEL_ID = "TimerServiceChannel"
        private const val NOTIF_ID = 1
        private const val TAG = "TimerService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action}")
        intent ?: return START_NOT_STICKY

        when (intent.action) {
            ACTION_START_TIMER -> {
                val taskId   = intent.getStringExtra(EXTRA_TASK_ID)
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "Активная задача"
                val prevAccum = intent.getLongExtra(EXTRA_ACCUMULATED_TIME, 0L)

                if (taskId == null) {
                    Log.w(TAG, "No taskId passed to START_TIMER")
                    stopSelf()
                } else {
                    // если уже идёт таймер по другой задаче — остановим его
                    if (timerJob?.isActive == true && currentTaskId != taskId) {
                        stopTimerTracking()
                    }
                    // если по этой же задаче уже запущен — обновим уведомление
                    if (timerJob?.isActive == true && currentTaskId == taskId) {
                        startForeground(NOTIF_ID, createNotification(taskName, accumulatedTimeMillis + (System.currentTimeMillis() - taskStartTimeMillis)))
                        return START_STICKY
                    }

                    currentTaskId = taskId
                    accumulatedTimeMillis = prevAccum
                    taskStartTimeMillis = System.currentTimeMillis()
                    startTimerTracking(taskName)
                    Log.d(TAG, "Timer START for $taskId ('$taskName') prevAccum=$prevAccum")
                }
            }

            ACTION_STOP_TIMER -> {
                val stopId = intent.getStringExtra(EXTRA_TASK_ID)
                if (stopId == null || stopId == currentTaskId) {
                    Log.d(TAG, "Received STOP_TIMER for $stopId (current=$currentTaskId)")
                    stopTimerTracking()
                    stopSelf()
                } else {
                    Log.d(TAG, "Ignoring STOP_TIMER for $stopId, current=$currentTaskId")
                }
            }
        }

        return START_STICKY
    }

    private fun startTimerTracking(taskName: String) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            startForeground(NOTIF_ID, createNotification(taskName, accumulatedTimeMillis))
            while (isActive) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - taskStartTimeMillis
                val total   = accumulatedTimeMillis + elapsed

                // Обновляем уведомление
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(NOTIF_ID, createNotification(taskName, total))

                // Сохраняем в БД
                currentTaskId?.let { id ->
                    // Получаем текущий объект
                    val item: ActivityItem? = activityRepository.getActivityById(id)
                    if (item != null) {
                        val updated = item.copy(
                            totalDurationMillisToday = total,
                            isActive = true
                        )
                        activityRepository.updateActivity(updated)
                        Log.d(TAG, "Saved time=$total for task '$id'")
                    }
                }
            }
        }
    }

    private fun stopTimerTracking() {
        timerJob?.cancel()
        timerJob = null

        // финальное сохранение
        currentTaskId?.let { id ->
            val finalTime = accumulatedTimeMillis + (if (taskStartTimeMillis > 0) System.currentTimeMillis() - taskStartTimeMillis else 0L)
            val item: ActivityItem? = runBlocking { activityRepository.getActivityById(id) }
            if (item != null) {
                val updated = item.copy(
                    totalDurationMillisToday = finalTime,
                    isActive = false
                )
                runBlocking { activityRepository.updateActivity(updated) }
                Log.d(TAG, "Final save time=$finalTime for task '$id'")
            }
        }

        currentTaskId = null
        taskStartTimeMillis = 0L
        accumulatedTimeMillis = 0L
        stopForeground(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Таймер задач",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NotificationManager::class.java))
                .createNotificationChannel(channel)
        }
    }

    private fun createNotification(taskName: String, elapsedMillis: Long): Notification {
        val fmt = formatDuration(elapsedMillis)
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val piOpen = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP_TIMER
            putExtra(EXTRA_TASK_ID, currentTaskId)
        }
        val piStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("Таймер: $taskName")
            .setContentText("Прошло: $fmt")
            .setSmallIcon(R.drawable.ic_launcher_foreground)        // замените на вашу иконку
            .setContentIntent(piOpen)
            .addAction(R.drawable.ic_stop, "Стоп", piStop)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun formatDuration(millis: Long): String {
        val s = (millis / 1000) % 60
        val m = (millis / (1000 * 60)) % 60
        val h = (millis / (1000 * 60 * 60))
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        serviceScope.cancel()
        stopTimerTracking()
    }

    // если не нужен биндинг — можно вернуть null
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
    override fun onBind(intent: Intent): IBinder? = binder
}