package kz.nurkanat.nurordertrack.data.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.EXECUTOR,
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now()
)

enum class UserRole(val value: String) {
    ADMIN("admin"),
    MANAGER("manager"),
    EXECUTOR("executor");

    companion object {
        fun fromString(value: String): UserRole =
            entries.find { it.value == value } ?: EXECUTOR
    }
}