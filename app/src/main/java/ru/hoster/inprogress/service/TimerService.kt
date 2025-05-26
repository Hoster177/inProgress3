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
import ru.hoster.inprogress.R // Убедись, что R импортирован правильно для доступа к иконке
// import ru.hoster.inprogress.data.local.ActivityDao // Раскомментируй, когда Dao будет готов
// import ru.hoster.inprogress.MainActivity // Раскомментируй и укажи правильный MainActivity

// Раскомментируй, когда DI будет настроен для сервиса
// @AndroidEntryPoint
class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timerJob: Job? = null
    private var currentTaskId: String? = null
    private var taskStartTimeMillis: Long = 0L
    private var accumulatedTimeMillis: Long = 0L // Время, уже накопленное для этой задачи до текущего запуска таймера

    // Раскомментируй, когда Dao будет внедрен через Hilt
    // @Inject lateinit var activityDao: ActivityDao

    companion object {
        const val ACTION_START_TIMER = "ACTION_START_TIMER"
        const val ACTION_STOP_TIMER = "ACTION_STOP_TIMER"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_NAME = "EXTRA_TASK_NAME" // Имя задачи для отображения в уведомлении
        const val EXTRA_ACCUMULATED_TIME = "EXTRA_ACCUMULATED_TIME" // Передача уже накопленного времени

        private const val NOTIFICATION_CHANNEL_ID = "TimerServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "TimerService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action: ${intent?.action}")
        intent?.let {
            when (it.action) {
                ACTION_START_TIMER -> {
                    val taskId = it.getStringExtra(EXTRA_TASK_ID)
                    val taskName = it.getStringExtra(EXTRA_TASK_NAME) ?: "Активная задача"
                    val previouslyAccumulatedTime = it.getLongExtra(EXTRA_ACCUMULATED_TIME, 0L)

                    if (taskId != null) {
                        // Если уже есть активный таймер для другой задачи, останавливаем его
                        if (timerJob?.isActive == true && currentTaskId != taskId) {
                            stopTimerTracking()
                        }
                        // Если таймер для этой же задачи уже запущен, ничего не делаем или перезапускаем с новым именем/временем
                        if (timerJob?.isActive == true && currentTaskId == taskId) {
                            Log.d(TAG, "Timer already running for task $taskId")
                            // Можно обновить уведомление, если, например, имя задачи изменилось
                            startForeground(NOTIFICATION_ID, createNotification(taskName, accumulatedTimeMillis + (System.currentTimeMillis() - taskStartTimeMillis)))
                            return START_STICKY
                        }

                        currentTaskId = taskId
                        accumulatedTimeMillis = previouslyAccumulatedTime
                        taskStartTimeMillis = System.currentTimeMillis()
                        startTimerTracking(taskName)
                        Log.d(TAG, "Started timer for task ID: $taskId, name: $taskName, accumulated: $previouslyAccumulatedTime")
                    } else {
                        Log.w(TAG, "Task ID is null, cannot start timer.")
                        stopSelf() // Останавливаем сервис, если ID задачи не предоставлен
                    }
                }
                ACTION_STOP_TIMER -> {
                    val taskIdToStop = it.getStringExtra(EXTRA_TASK_ID)
                    // Останавливаем таймер только если ID совпадает или если ID не указан (общая команда стоп)
                    if (taskIdToStop == null || taskIdToStop == currentTaskId) {
                        Log.d(TAG, "Stopping timer for task ID: $currentTaskId")
                        stopTimerTracking()
                        stopSelf() // Останавливаем сервис после остановки таймера
                    } else {
                        Log.d(TAG, "Stop command for different task ID ($taskIdToStop), current is $currentTaskId. Ignoring.")
                    }
                }

                else -> {}
            }
        }
        return START_STICKY // Сервис будет перезапущен, если система его убьет
    }

    private fun startTimerTracking(taskName: String) {
        timerJob?.cancel() // Отменяем предыдущий Job, если он был
        timerJob = serviceScope.launch {
            Log.d(TAG, "Timer tracking started in coroutine for $taskName")
            startForeground(NOTIFICATION_ID, createNotification(taskName, accumulatedTimeMillis))

            while (isActive) {
                delay(1000) // Обновляем каждую секунду
                val elapsedTime = System.currentTimeMillis() - taskStartTimeMillis
                val totalTrackedTime = accumulatedTimeMillis + elapsedTime

                // Обновляем уведомление
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, createNotification(taskName, totalTrackedTime))

                // Здесь будет логика сохранения `totalTrackedTime` в базу данных для `currentTaskId`
                // Пример:
                // currentTaskId?.let { taskId ->
                //    activityDao.updateTaskTime(taskId, totalTrackedTime)
                //    Log.d(TAG, "Updated time for task $taskId to $totalTrackedTime")
                // }
                Log.d(TAG, "Task $currentTaskId ($taskName) - Elapsed: ${formatDuration(totalTrackedTime)}")
            }
        }
    }

    private fun stopTimerTracking() {
        timerJob?.cancel()
        timerJob = null
        // Сохраняем финальное время перед остановкой
        currentTaskId?.let { taskId ->
            val finalTime = accumulatedTimeMillis + (if (taskStartTimeMillis > 0) System.currentTimeMillis() - taskStartTimeMillis else 0)
            // activityDao.updateTaskTime(taskId, finalTime) // Сохранить финальное время
            Log.d(TAG, "Timer stopped for task $taskId. Final time: ${formatDuration(finalTime)}")
        }
        currentTaskId = null
        taskStartTimeMillis = 0L
        accumulatedTimeMillis = 0L
        stopForeground(true) // Убираем уведомление, когда таймер полностью остановлен
        // или можно оставить stopSelf(), который вызовет onDestroy, где будет stopForeground
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Канал Сервиса Таймера", // Имя канала, видимое пользователю
                NotificationManager.IMPORTANCE_LOW // LOW или DEFAULT, чтобы не было звука при каждом обновлении
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(taskName: String, elapsedTimeMillis: Long): Notification {
        // Интент для открытия приложения по клику на уведомление
        val notificationIntent = Intent(this, MainActivity::class.java) // Укажи свой MainActivity
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // Интент для кнопки "Стоп"
        val stopTimerIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP_TIMER
            putExtra(EXTRA_TASK_ID, currentTaskId) // Передаем ID текущей задачи для остановки
        }
        val stopTimerPendingIntent = PendingIntent.getService(
            this, 0, stopTimerIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Таймер активен: $taskName")
            .setContentText("Прошло: ${formatDuration(elapsedTimeMillis)}")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Замени на свою иконку уведомлений
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Стоп", stopTimerPendingIntent) // Замени ic_stop на свою иконку
            .setOnlyAlertOnce(true) // Не беспокоить пользователя при каждом обновлении времени
            .setOngoing(true) // Уведомление нельзя смахнуть
            .build()
    }

    // Вспомогательная функция для форматирования времени
    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }


    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        serviceScope.cancel() // Отменяем все корутины в скоупе сервиса
        stopTimerTracking() // Убедимся, что таймер точно остановлен и время сохранено
        stopForeground(true) // Убираем уведомление
        super.onDestroy()
    }

    // Для биндинга, если не планируешь использовать, можно оставить так
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
    override fun onBind(intent: Intent): IBinder? {
        return binder // или null, если биндинг не нужен
    }
}
