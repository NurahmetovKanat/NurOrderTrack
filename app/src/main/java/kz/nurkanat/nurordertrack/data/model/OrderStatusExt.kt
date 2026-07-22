package kz.nurkanat.nurordertrack.data.model

import android.content.Context
import kz.nurkanat.nurordertrack.R

fun OrderStatus.displayName(context: Context): String = context.getString(
    when (this) {
        OrderStatus.NEW -> R.string.status_new
        OrderStatus.IN_PROGRESS -> R.string.status_in_progress
        OrderStatus.DONE -> R.string.status_done
        OrderStatus.CLOSED -> R.string.status_closed
        OrderStatus.CANCELLED -> R.string.status_cancelled
    }
)