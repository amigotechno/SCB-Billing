package com.scb.scbbillingandcollection.auth.data.repository

import com.scb.scbbillingandcollection.auth.data.models.LoginRequest
import com.scb.scbbillingandcollection.auth.data.models.LoginResponse
import com.scb.scbbillingandcollection.auth.data.models.VersionResponse
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.generate_bill.data.models.WardsResponse

interface AuthRepository {

    suspend fun getVersionCheck(): Resource<VersionResponse>
    suspend fun loginCheck(request: LoginRequest): Resource<LoginResponse>
}