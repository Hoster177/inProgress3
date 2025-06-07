package ru.hoster.inprogress.domain.model

import kotlinx.coroutines.flow.Flow
import ru.hoster.inprogress.data.Goal



interface GoalRepository {
    suspend fun getGoals(userId: String): Result<List<Goal>>
    suspend fun addGoal(goal: Goal): Result<Unit>
    suspend fun updateGoal(goal: Goal): Result<Unit>
    suspend fun deleteGoal(goalId: String): Result<Unit>
    suspend fun getGoalById(goalId: String): Result<Goal?>
    suspend fun getActiveGoalsFlow(): Flow<List<Goal>>
}
