package kz.nurkanat.nurordertrack.data.model

import com.google.firebase.Timestamp

data class Client(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val note: String = "",
    val createdAt: Timestamp = Timestamp.now()
)