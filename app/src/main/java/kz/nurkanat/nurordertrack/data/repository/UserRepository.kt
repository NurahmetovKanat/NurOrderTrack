package kz.nurkanat.nurordertrack.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kz.nurkanat.nurordertrack.data.model.User
import kz.nurkanat.nurordertrack.data.model.UserRole

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    // Текущий залогиненный пользователь
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Получить данные текущего пользователя как Flow
    fun getCurrentUserFlow(): Flow<User?> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = usersCollection.document(uid)
            .addSnapshotListener { snapshot, _ ->
                val user = snapshot?.toUser()
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    // Получить всех пользователей
    fun getAllUsersFlow(): Flow<List<User>> = callbackFlow {
        val listener = usersCollection
            .addSnapshotListener { snapshot, _ ->
                val users = snapshot?.documents?.mapNotNull { it.toUser() } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    // Создать пользователя (только Админ)
    suspend fun createUser(name: String, email: String, password: String, role: UserRole): Result<Unit> {
        return try {
            // Создаём аккаунт в Firebase Auth
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("UID не получен"))

            // Сохраняем в Firestore
            val user = User(id = uid, name = name, email = email, role = role)
            usersCollection.document(uid).set(user.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Обновить пользователя
    suspend fun updateUser(userId: String, name: String, role: UserRole): Result<Unit> {
        return try {
            usersCollection.document(userId).update(
                mapOf("name" to name, "role" to role.value)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Деактивировать пользователя
    suspend fun deactivateUser(userId: String, isActive: Boolean): Result<Unit> {
        return try {
            usersCollection.document(userId).update("isActive", isActive).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Войти
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Выйти
    fun signOut() = auth.signOut()

    // Проверка — залогинен ли пользователь
    fun isLoggedIn(): Boolean = auth.currentUser != null

    // Создать дефолтного Админа при первом запуске
    suspend fun createDefaultAdminIfNeeded() {
        try {
            // Сначала тихо входим как анонимный — нет, просто создаём через Auth напрямую
            // Проверяем только после входа
            val currentUser = auth.currentUser ?: return

            val snapshot = usersCollection
                .whereEqualTo("role", "admin")
                .get().await()

            if (snapshot.isEmpty) {
                createUser(
                    name = "Администратор",
                    email = "admin@nurordertrack.kz",
                    password = "Admin123!",
                    role = UserRole.ADMIN
                )
            }
        } catch (e: Exception) {
            // Игнорируем ошибку — админ будет создан позже
        }
    }
}

// Extension функции для конвертации
fun com.google.firebase.firestore.DocumentSnapshot.toUser(): User? {
    return try {
        User(
            id = id,
            name = getString("name") ?: "",
            email = getString("email") ?: "",
            role = UserRole.fromString(getString("role") ?: ""),
            isActive = getBoolean("isActive") ?: true,
            createdAt = getTimestamp("createdAt")
                ?: com.google.firebase.Timestamp.now()
        )
    } catch (e: Exception) { null }
}

fun User.toMap(): Map<String, Any> = mapOf(
    "name" to name,
    "email" to email,
    "role" to role.value,
    "isActive" to isActive,
    "createdAt" to createdAt
)