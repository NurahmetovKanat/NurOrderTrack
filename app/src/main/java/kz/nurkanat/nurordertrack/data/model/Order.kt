package kz.nurkanat.nurordertrack.data.model

import com.google.firebase.Timestamp

data class Order(
    val id: String = "",
    val orderNumber: Int = 0,
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val comment: String = "",
    val status: OrderStatus = OrderStatus.NEW,
    val createdBy: String = "",
    val assignedTo: String = "",
    val assignedToName: String = "",
    val totalAmount: Double = 0.0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

enum class OrderStatus(val value: String) {
    NEW("new"),
    IN_PROGRESS("in_progress"),
    DONE("done"),
    CLOSED("closed"),
    CANCELLED("cancelled");

    companion object {
        fun fromString(value: String): OrderStatus =
            entries.find { it.value == value } ?: NEW
    }
}