package kz.nurkanat.nurordertrack.data.model

data class OrderItem(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Double = 1.0,
    val price: Double = 0.0
) {
    val total: Double get() = quantity * price
}