package com.scb.scbbillingandcollection.core.retrofit

import okhttp3.ResponseBody

sealed class Resource<out T> {
    data class Success<out T>(val value: T) : Resource<T>()

    data class Failure(
        val isNetworkError: Boolean,
        val errorCode: Int? = null,
        val errorBody: ResponseBody? = null,
        val throwable: Throwable
    ) : Resource<Nothing>() {
        val body = errorBody?.byteString()?.utf8()
    }

    object Loading : Resource<Nothing>()
}

