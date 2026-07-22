package kz.nurkanat.nurordertrack.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kz.nurkanat.nurordertrack.data.model.Client

class ClientRepository {
    private val db = FirebaseFirestore.getInstance()
    private val clientsCollection = db.collection("clients")

    fun getAllClientsFlow(): Flow<List<Client>> = callbackFlow {
        val listener = clientsCollection
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val clients = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Client(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            phone = doc.getString("phone") ?: "",
                            note = doc.getString("note") ?: "",
                            createdAt = doc.getTimestamp("createdAt")
                                ?: com.google.firebase.Timestamp.now()
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(clients)
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveClient(client: Client): Result<Unit> {
        return try {
            val map = mapOf(
                "name" to client.name,
                "phone" to client.phone,
                "note" to client.note,
                "createdAt" to client.createdAt
            )
            if (client.id.isEmpty()) {
                clientsCollection.add(map).await()
            } else {
                clientsCollection.document(client.id).set(map).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteClient(clientId: String): Result<Unit> {
        return try {
            clientsCollection.document(clientId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}