package kz.nurkanat.nurordertrack.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kz.nurkanat.nurordertrack.data.model.Product
import kz.nurkanat.nurordertrack.data.model.ProductType

class ProductRepository {
    private val db = FirebaseFirestore.getInstance()
    private val productsCollection = db.collection("products")

    fun getAllProductsFlow(): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .addSnapshotListener { snapshot, _ ->
                val products = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Product(
                            id         = doc.id,
                            name       = doc.getString("name") ?: "",
                            price      = doc.getDouble("price") ?: 0.0,
                            unit       = doc.getString("unit") ?: "шт",
                            // ── читаем тип из Firestore ──
                            type       = ProductType.fromString(doc.getString("type") ?: ""),
                            isArchived = doc.getBoolean("isArchived") ?: false
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(products)
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveProduct(product: Product): Result<Unit> {
        return try {
            val map = mapOf(
                "name"       to product.name,
                "price"      to product.price,
                "unit"       to product.unit,
                // ── сохраняем тип в Firestore ──
                "type"       to product.type.value,
                "isArchived" to product.isArchived
            )
            if (product.id.isEmpty()) {
                productsCollection.add(map).await()
            } else {
                productsCollection.document(product.id).set(map).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveProduct(productId: String, isArchived: Boolean): Result<Unit> {
        return try {
            productsCollection.document(productId)
                .update("isArchived", isArchived).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}