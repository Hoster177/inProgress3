package ru.hoster.inprogress.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.hoster.inprogress.data.Goal
import ru.hoster.inprogress.domain.model.GoalRepository
import ru.hoster.inprogress.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
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
