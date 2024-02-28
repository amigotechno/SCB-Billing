package com.scb.scbbillingandcollection.auth.data.repository

import com.scb.scbbillingandcollection.auth.data.models.LoginRequest
import com.scb.scbbillingandcollection.auth.data.models.LoginResponse
import com.scb.scbbillingandcollection.auth.data.models.VersionResponse
import com.scb.scbbillingandcollection.core.retrofit.ApiInterface
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.core.retrofit.SafeApiCall
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(private val api: ApiInterface) : SafeApiCall,
    AuthRepository {

    override suspend fun getVersionCheck(): Resource<VersionResponse> {
        val response = safeApiCall {
            api.getPoliceStations("0")
        }
        return if (response is Resource.Success) {
            Resource.Success(response.value)
        } else response
    }

    override suspend fun loginCheck(request: LoginRequest): Resource<LoginResponse> {
        val response = safeApiCall {
            api.checkLogin(request)
        }
        return if (response is Resource.Success) {
            Resource.Success(response.value)
        } else response
    }

}