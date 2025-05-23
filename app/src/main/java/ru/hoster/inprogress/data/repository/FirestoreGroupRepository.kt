package ru.hoster.inprogress.data.repository // Убедитесь, что пакет правильный

import ru.hoster.inprogress.domain.model.GroupData
import ru.hoster.inprogress.domain.model.GroupRepository
import ru.hoster.inprogress.domain.model.Result
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreGroupRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : GroupRepository {

    companion object {
        private const val GROUPS_COLLECTION = "groups"
    }

    override suspend fun getGroupById(groupId: String): Result<GroupData?> { // Этот метод остается как был
        return try {
            val documentSnapshot = firestore.collection(GROUPS_COLLECTION).document(groupId).get().await()
            val group = documentSnapshot.toObject(GroupData::class.java)
            Result.Success(group)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getGroupsForUser(userId: String): Result<List<GroupData>> { // Этот метод остается как был
        return try {
            val querySnapshot = firestore.collection(GROUPS_COLLECTION)
                .whereArrayContains("memberUserIds", userId)
                .get()
                .await()
            val groups = querySnapshot.documents.mapNotNull { it.toObject(GroupData::class.java) }
            Result.Success(groups)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Метод insertGroup из вашего предыдущего сообщения, но теперь исправленный
    override suspend fun insertGroup(group: GroupData): Result<String> {
        return try {
            val documentRef = firestore.collection(GROUPS_COLLECTION).document() // Авто-генерация ID
            // 'group.createdAt' должен быть null, чтобы @ServerTimestamp сработал.
            // Копируем 'id' в объект, предполагая, что 'createdAt' уже null по умолчанию в GroupData.
            val groupToSave = group.copy(id = documentRef.id)
            documentRef.set(groupToSave).await()
            Result.Success(documentRef.id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateGroup(group: GroupData): Result<Unit> { // Этот метод остается как был
        return try {
            firestore.collection(GROUPS_COLLECTION).document(group.id)
                .set(group, SetOptions.merge())
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeUserFromGroup(groupId: String, userId: String): Result<Unit> { // Этот метод остается как был
        return try {
            firestore.collection(GROUPS_COLLECTION).document(groupId)
                .update("memberUserIds", FieldValue.arrayRemove(userId))
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun addUserToGroup(groupId: String, userId: String): Result<Unit> { // Этот метод остается как был
        return try {
            firestore.collection(GROUPS_COLLECTION).document(groupId)
                .update("memberUserIds", FieldValue.arrayUnion(userId))
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}