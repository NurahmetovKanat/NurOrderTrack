package kz.nurkanat.nurordertrack.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kz.nurkanat.nurordertrack.data.model.Order
import kz.nurkanat.nurordertrack.data.model.OrderItem
import kz.nurkanat.nurordertrack.data.model.OrderLog
import kz.nurkanat.nurordertrack.data.model.OrderStatus

class OrderRepository {
    private val db = FirebaseFirestore.getInstance()
    private val ordersCollection = db.collection("orders")

    fun getAllOrdersFlow(): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val orders = snapshot?.documents?.mapNotNull { it.toOrder() } ?: emptyList()
                trySend(orders)
            }
        awaitClose { listener.remove() }
    }

    fun getOrdersByExecutorFlow(executorId: String): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("assignedTo", executorId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val orders = snapshot?.documents?.mapNotNull { it.toOrder() } ?: emptyList()
                trySend(orders)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createOrder(order: Order, items: List<OrderItem>): Result<String> {
        return try {
            val orderMap = order.toMap()
            val docRef = ordersCollection.add(orderMap).await()
            val orderId = docRef.id
            items.forEach { item ->
                val itemMap = mapOf(
                    "productId" to item.productId,
                    "productName" to item.productName,
                    "quantity" to item.quantity,
                    "price" to item.price
                )
                ordersCollection.document(orderId)
                    .collection("items").add(itemMap).await()
            }
            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrder(order: Order, items: List<OrderItem>): Result<Unit> {
        return try {
            ordersCollection.document(order.id).set(order.toMap()).await()
            val itemsRef = ordersCollection.document(order.id).collection("items")
            val oldItems = itemsRef.get().await()
            oldItems.forEach { it.reference.delete().await() }
            items.forEach { item ->
                val itemMap = mapOf(
                    "productId" to item.productId,
                    "productName" to item.productName,
                    "quantity" to item.quantity,
                    "price" to item.price
                )
                itemsRef.add(itemMap).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        changedByName: String,
        oldStatus: OrderStatus,
        changedById: String
    ): Result<Unit> {
        return try {
            ordersCollection.document(orderId).update(
                mapOf(
                    "status" to newStatus.value,
                    "updatedAt" to Timestamp.now()
                )
            ).await()
            addLog(
                orderId = orderId,
                action = "status_changed",      // ✅ ключ вместо переведённого текста
                changedBy = changedById,
                changedByName = changedByName,
                oldValue = oldStatus.value,     // ✅ "new" вместо "Новый"
                newValue = newStatus.value      // ✅ "in_progress" вместо "В работе"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOrder(orderId: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderItems(orderId: String): List<OrderItem> {
        return try {
            val snapshot = ordersCollection.document(orderId)
                .collection("items").get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    OrderItem(
                        id = doc.id,
                        productId = doc.getString("productId") ?: "",
                        productName = doc.getString("productName") ?: "",
                        quantity = doc.getDouble("quantity") ?: 1.0,
                        price = doc.getDouble("price") ?: 0.0
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    fun getOrderLogsFlow(orderId: String): Flow<List<OrderLog>> = callbackFlow {
        val listener = ordersCollection.document(orderId)
            .collection("logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val logs = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        OrderLog(
                            id = doc.id,
                            action = doc.getString("action") ?: "",
                            changedBy = doc.getString("changedBy") ?: "",
                            changedByName = doc.getString("changedByName") ?: "",
                            oldValue = doc.getString("oldValue") ?: "",
                            newValue = doc.getString("newValue") ?: "",
                            timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(logs)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addLog(
        orderId: String,
        action: String,
        changedBy: String,
        changedByName: String,
        oldValue: String,
        newValue: String
    ) {
        try {
            val log = mapOf(
                "action" to action,
                "changedBy" to changedBy,
                "changedByName" to changedByName,
                "oldValue" to oldValue,
                "newValue" to newValue,
                "timestamp" to Timestamp.now()
            )
            ordersCollection.document(orderId).collection("logs").add(log).await()
        } catch (_: Exception) {}
    }

    suspend fun getNextOrderNumber(): Int {
        return try {
            val snapshot = db.collection("meta")
                .document("counters")
                .get().await()
            val current = snapshot.getLong("orderCount") ?: 0
            val next = current + 1
            db.collection("meta")
                .document("counters")
                .set(mapOf("orderCount" to next))
                .await()
            next.toInt()
        } catch (e: Exception) {
            1
        }
    }
}

fun com.google.firebase.firestore.DocumentSnapshot.toOrder(): Order? {
    return try {
        Order(
            id = id,
            orderNumber = getLong("orderNumber")?.toInt() ?: 0,
            clientId = getString("clientId") ?: "",
            clientName = getString("clientName") ?: "",
            clientPhone = getString("clientPhone") ?: "",
            comment = getString("comment") ?: "",
            status = OrderStatus.fromString(getString("status") ?: ""),
            createdBy = getString("createdBy") ?: "",
            assignedTo = getString("assignedTo") ?: "",
            assignedToName = getString("assignedToName") ?: "",
            totalAmount = getDouble("totalAmount") ?: 0.0,
            createdAt = getTimestamp("createdAt") ?: Timestamp.now(),
            updatedAt = getTimestamp("updatedAt") ?: Timestamp.now()
        )
    } catch (e: Exception) { null }
}

fun Order.toMap(): Map<String, Any> = mapOf(
    "orderNumber" to orderNumber,
    "clientId" to clientId,
    "clientName" to clientName,
    "clientPhone" to clientPhone,
    "comment" to comment,
    "status" to status.value,
    "createdBy" to createdBy,
    "assignedTo" to assignedTo,
    "assignedToName" to assignedToName,
    "totalAmount" to totalAmount,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)