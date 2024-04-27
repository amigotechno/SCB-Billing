package com.scb.scbbillingandcollection.core.extensions

import android.location.Address
import android.location.Location
import android.os.Build
import com.birjuvachhani.locus.Locus
import com.google.android.gms.location.Priority
import java.util.concurrent.TimeUnit

const val LOCATION_INTERVAL = 3000L

const val VALID_LOCATION_ACCURACY = 20F

fun Locus.configureLocus(
    requireBackgroundUpdate: Boolean = false, forceRequireBackgroundUpdate: Boolean = false
): Locus {
    return this.apply {
        configure {
            run {
                enableBackgroundUpdates = requireBackgroundUpdate
                forceBackgroundUpdates = forceRequireBackgroundUpdate
            }
            request {
                interval = LOCATION_INTERVAL
                fastestInterval = LOCATION_INTERVAL.div(2)
                priority = Priority.PRIORITY_HIGH_ACCURACY
                isWaitForAccurateLocation = true
            }
        }
    }
}

fun Address?.getOrNull(position: Int): String? {
    this?.let {
        if (maxAddressLineIndex >= position) {
            return getAddressLine(position)
        }
    }
    return null
}

fun Location.isInvalidLocation() = this.accuracy > VALID_LOCATION_ACCURACY || this.isMockLocation()

@Suppress("DEPRECATION")
fun Location.isMockLocation() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    this.isMock
} else {
    this.isFromMockProvider
}


fun Long.addTimeForAlarm(interval: Long): Long {

    return this + TimeUnit.SECONDS.toMillis(interval)
//    return (this + TimeUnit.SECONDS.toMillis(
////        maxOf(
////            interval ?: CpConstants.CP_INTERVAL_ON_ERROR_CASE,
////            CpConstants.CP_INTERVAL_ON_ERROR_CASE
////        )
//    ))
}



