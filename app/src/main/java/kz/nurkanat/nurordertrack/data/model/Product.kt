package kz.nurkanat.nurordertrack.data.model

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val unit: String = "шт",   // используется только для PRODUCT
    val type: ProductType = ProductType.PRODUCT,
    val isArchived: Boolean = false
)

enum class ProductType(val value: String) {
    PRODUCT("product"),
    SERVICE("service");

    companion object {
        fun fromString(value: String): ProductType =
            entries.find { it.value == value } ?: PRODUCT
    }
}