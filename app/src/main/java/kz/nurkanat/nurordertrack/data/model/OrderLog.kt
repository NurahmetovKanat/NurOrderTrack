package kz.nurkanat.nurordertrack.data.model

import com.google.firebase.Timestamp

data class OrderLog(
    val id: String = "",
    val action: String = "",
    val changedBy: String = "",
    val changedByName: String = "",
    val oldValue: String = "",
    val newValue: String = "",
    val timestamp: Timestamp = Timestamp.now()
)