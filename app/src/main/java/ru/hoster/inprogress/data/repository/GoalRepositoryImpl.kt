package ru.hoster.inprogress.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.hoster.inprogress.data.Goal
import ru.hoster.inprogress.domain.model.GoalRepository
import ru.hoster.inprogress.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Важно для Hilt, если репозиторий должен быть синглтоном
class GoalRepositoryImpl @Inject constructor(
    // Сюда можно внедрить зависимости, например, DAO для Room или сервис для Firestore
    // private val goalDao: GoalDao, // Пример для локальной базы данных
    // private val firestoreService: YourFirestoreServiceForGoals // Пример для удаленного источника
) : GoalRepository {
    override suspend fun getGoals(userId: String): Result<List<Goal>> {
        TODO("Not yet implemented")
    }

    override suspend fun addGoal(goal: Goal): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun updateGoal(goal: Goal): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteGoal(goalId: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getGoalById(goalId: String): Result<Goal?> {
        TODO("Not yet implemented")
    }

    override suspend fun getActiveGoalsFlow(): Flow<List<Goal>> {
        return flow { emit(emptyList<Goal>()) }
    }

}
