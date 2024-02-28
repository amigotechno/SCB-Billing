package com.scb.scbbillingandcollection.core.extensions

import android.os.SystemClock
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import com.google.gson.Gson

private const val DEBOUNCE_TIME = 1000L

/**
 * Avoid unwanted multiple clicks on a view
 */
fun View.clickWithDebounce(
    checkInternetConnection: Boolean = false,
    debounceTime: Long = DEBOUNCE_TIME,
    action: () -> Unit
) {
    setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            when (SystemClock.elapsedRealtime() - lastClickTime < debounceTime) {
                true -> return
                false -> {
                    if (checkInternetConnection && !v.context.isNetworkAvailable()) {

                    } else {
                        action()
                    }
                }
            }
            lastClickTime = SystemClock.elapsedRealtime()
        }
    })
}

private fun Any.toJson() = Gson().toJson(this)
fun <T> diffChecker(areItemsTheSame: (T, T) -> Boolean): DiffUtil.ItemCallback<T> where T : Any {
    return object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
            return areItemsTheSame.invoke(oldItem, newItem)
        }

        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
            return oldItem.toJson() == newItem.toJson()
        }

    }
}