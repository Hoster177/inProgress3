package ru.hoster.inprogress.service

import android.util.Log // Добавьте импорт логов
import ru.hoster.inprogress.data.TimerSession
import ru.hoster.inprogress.data.local.TimerSessionDao
import ru.hoster.inprogress.data.repository.FirestoreSessionRepository
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest // Для переключения таймера
import kotlinx.coroutines.flow.flow // Для создания тикающего Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ActiveTimerInfo(
    val activityId: Long, // Это локальный ID активности
    val startTime: Long, // Время начала в миллисекундах
    val currentDurationMillis: Long
)

@Singleton
class TimerService @Inject constructor(
    private val timerSessionDao: TimerSessionDao,
    private val firestoreSessionRepository: FirestoreSessionRepository
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob()) // Используем SupervisorJob
    private val firestoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Хранит ID текущей активной сессии в локальной БД (не TimerSession.id, а activityId)
    // и время ее старта.
    private val _activeLocalSessionInfo = MutableStateFlow<Pair<Long, Long>?>(null) // Pair<activityId, startTimeMillis>

    private var tickerJob: Job? = null

    // Flow, который эмитит информацию о текущем активном таймере (или null)
    private val _activeTimerFlow = MutableStateFlow<ActiveTimerInfo?>(null)
    val activeTimerFlow: StateFlow<ActiveTimerInfo?> = _activeTimerFlow.asStateFlow()


    // При инициализации сервиса, проверим, нет ли уже активной сессии в БД
    // Это важно, если сервис был убит и перезапущен, а таймер шел
    init {
        serviceScope.launch {
            // Нужен способ получить userId при старте сервиса.
            // Это ограничение, если TimerService не имеет прямого доступа к AuthService.
            // Пока оставим это TODO или предположим, что userId передается при каждом вызове.
            // Для простоты, сейчас эта логика не будет восстанавливать таймер при перезапуске сервиса,
            // а будет полагаться на явные вызовы start/stop.
            // Если нужно восстановление, то при старте сервиса нужно будет для текущего userId
            // найти активную сессию в DAO и запустить _activeLocalSessionInfo.value = Pair(it.activityId, it.startTime.time)
        }
    }


    suspend fun startTimer(activityLocalId: Long, userId: String, activityFirebaseId: String): TimerSession {
        Log.i("TimerService_Debug", "Attempting to START timer. activityLocalId: $activityLocalId, userId: '$userId', activityFirebaseId: '$activityFirebaseId'")

        if (activityLocalId == 0L) {
            Log.e("TimerService_Debug", "CRITICAL_ERROR: activityLocalId is 0L.")
            throw IllegalArgumentException("activityLocalId cannot be 0L")
        }
        if (userId.isBlank()) {
            Log.e("TimerService_Debug", "CRITICAL_ERROR: userId is blank.")
            throw IllegalArgumentException("userId cannot be blank")
        }
        if (activityFirebaseId.isBlank()) {
            Log.e("TimerService_Debug", "CRITICAL_ERROR: activityFirebaseId is blank.")
            throw IllegalArgumentException("activityFirebaseId cannot be blank for Firestore operations")
        }

        val existingActiveSessionForThisActivity = timerSessionDao.getActiveSession(userId, activityLocalId)
        if (existingActiveSessionForThisActivity != null) {
            Log.w("TimerService_Debug", "startTimer: Timer is ALREADY ACTIVE for this specific activityLocalId $activityLocalId, userId '$userId'. Session ID: ${existingActiveSessionForThisActivity.id}")
            _activeLocalSessionInfo.value = Pair(existingActiveSessionForThisActivity.activityId, existingActiveSessionForThisActivity.startTime.time)
            startUiUpdater()
            return existingActiveSessionForThisActivity
        }

        val newSession = TimerSession(
            activityId = activityLocalId, // Локальный ID для связи в Room
            userId = userId,
            startTime = Date(),
            endTime = null
        )
        Log.d("TimerService_Debug", "startTimer: Creating new session object: LocalActivityID=$activityLocalId, UserID='$userId', StartTime=${newSession.startTime}")

        val generatedLocalSessionId = timerSessionDao.insertSession(newSession)
        val createdSession = newSession.copy(id = generatedLocalSessionId)
        Log.i("TimerService_Debug", "startTimer: New session INSERTED LOCALLY. LocalSessionID: $generatedLocalSessionId, ForLocalActivityID: $activityLocalId, UserID: '$userId'")

        _activeLocalSessionInfo.value = Pair(createdSession.activityId, createdSession.startTime.time)
        startUiUpdater()

        firestoreScope.launch {
            try {
                Log.d("TimerService_Firestore", "startTimer: Attempting to add session to Firestore. ForFirebaseActivityID: '$activityFirebaseId', UserID_orig: '$userId'")
                // Используем НОВУЮ сигнатуру addSession, передавая activityFirebaseId
                firestoreSessionRepository.addSession(userId, activityFirebaseId, createdSession)
                Log.i("TimerService_Firestore", "startTimer: Session successfully added to Firestore for FirebaseActivityID '$activityFirebaseId', UserID '$userId'.")
            } catch (e: Exception) {
                Log.e("TimerService_Firestore", "startTimer: ERROR syncing new session to Firestore for FirebaseActivityID '$activityFirebaseId': ${e.message}", e)
            }
        }
        return createdSession
    }

    suspend fun stopTimer(activityLocalId: Long, userId: String, activityFirebaseId: String): TimerSession? {
        Log.i("TimerService_Debug", "Attempting to STOP timer. activityLocalId: $activityLocalId, userId: '$userId', activityFirebaseId: '$activityFirebaseId'")

        val activeSession = timerSessionDao.getActiveSession(userId, activityLocalId)
        if (activeSession != null) {
            val newEndTime = Date()
            val updatedSession = activeSession.copy(endTime = newEndTime)
            timerSessionDao.updateSession(updatedSession)
            Log.i("TimerService_Debug", "stopTimer: Session UPDATED LOCALLY for LocalActivityID: $activityLocalId")

            if (_activeLocalSessionInfo.value?.first == activityLocalId) {
                _activeLocalSessionInfo.value = null
                stopUiUpdater()
            }

            firestoreScope.launch {
                try {
                    Log.d("TimerService_Firestore", "stopTimer: Attempting to update session in Firestore. ForFirebaseActivityID: '$activityFirebaseId', UserID: '$userId', SessionStartTime: ${activeSession.startTime}")
                    // Используем НОВУЮ сигнатуру updateSessionEndTime
                    firestoreSessionRepository.updateSessionEndTime(userId, activityFirebaseId, activeSession.startTime, newEndTime)
                    Log.i("TimerService_Firestore", "stopTimer: Session successfully updated in Firestore for FirebaseActivityID '$activityFirebaseId'.")
                } catch (e: Exception) {
                    Log.e("TimerService_Firestore", "stopTimer: ERROR syncing session stop to Firestore for FirebaseActivityID '$activityFirebaseId': ${e.message}", e)
                }
            }
            return updatedSession
        } else {
            Log.w("TimerService_Debug", "stopTimer: No active session found in DAO for localActivityId $activityLocalId, userId '$userId'.")
            if (_activeLocalSessionInfo.value?.first == activityLocalId) {
                _activeLocalSessionInfo.value = null
                stopUiUpdater()
            }
            return null
        }
    }


    // Вызывается при старте таймера
    private fun startUiUpdater() {
        stopUiUpdater() // Остановить предыдущий, если был
        tickerJob = serviceScope.launch {
            _activeLocalSessionInfo.collect { sessionInfoPair ->
                if (sessionInfoPair != null) {
                    val (currentLocalActivityId, startTimeMillis) = sessionInfoPair
                    Log.d("TimerService_Ticker", "UI Updater started for localActivityId: $currentLocalActivityId, startTime: $startTimeMillis")
                    while (this.isActive && _activeLocalSessionInfo.value?.first == currentLocalActivityId) {
                        val duration = System.currentTimeMillis() - startTimeMillis
                        _activeTimerFlow.value = ActiveTimerInfo(currentLocalActivityId, startTimeMillis, duration)
                        delay(1000)
                    }
                } else {
                    if (_activeTimerFlow.value != null) {
                        Log.d("TimerService_Ticker", "No active session info, clearing activeTimerFlow.")
                        _activeTimerFlow.value = null
                    }
                }
            }
        }
        Log.d("TimerService_Ticker", "Main tickerJob launched.")
    }

    private fun stopUiUpdater() {
        tickerJob?.cancel()
        tickerJob = null
        if (_activeTimerFlow.value != null) {
            _activeTimerFlow.value = null
        }
        Log.d("TimerService_Ticker", "UI Updater (tickerJob) stopped.")
    }

    suspend fun getActiveTimerSession(activityLocalId: Long, userId: String): TimerSession? {
        Log.d("TimerService_Debug", "getActiveTimerSession called. activityLocalId_param: $activityLocalId, userId_param: '$userId'")
        return timerSessionDao.getActiveSession(userId, activityLocalId) // Используем локальный ID
    }

    // Остальные методы сервиса (getAllSessionsForUser и т.д.) остаются как есть
    fun getAllSessionsForUser(userId: String): Flow<List<TimerSession>> {
        return timerSessionDao.getAllSessionsFlow(userId)
    }

    fun getSessionsForUserInDateRange(userId: String, from: Date, to: Date): Flow<List<TimerSession>> {
        return timerSessionDao.getSessionsForDateRangeFlow(userId, from, to)
    }

    fun getAllSessionsForActivity(userId: String, activityId: Long): Flow<List<TimerSession>> {
        return timerSessionDao.getSessionsForActivityFlow(userId, activityId)
    }

}