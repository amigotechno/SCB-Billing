package com.scb.scbbillingandcollection.core.retrofit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.withContext
import retrofit2.HttpException

interface SafeApiCall {
    suspend fun <T> safeApiCall(
        flowCollector: FlowCollector<Resource<T>>? = null,
        emitLoadingState: Boolean = false,
        apiCall: suspend () -> T
    ): Resource<T> {
        flowCollector?.let {
            if (emitLoadingState)
                flowCollector.emit(Resource.Loading)
        }
        return withContext(Dispatchers.IO) {
            try {
                Resource.Success(apiCall.invoke())
            } catch (throwable: Throwable) {
                when (throwable) {
                    is HttpException -> {
                        Resource.Failure(
                            false,
                            throwable.code(),
                            throwable.response()?.errorBody(),
                            throwable
                        )
                    }

                    else -> {
                        Resource.Failure(true, null, null, throwable)
                    }
                }
            }
        }
    }
}
