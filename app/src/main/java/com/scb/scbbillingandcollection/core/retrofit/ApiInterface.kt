package com.scb.scbbillingandcollection.core.retrofit

import com.scb.scbbillingandcollection.generate_bill.data.models.ConsumerListResponse
import com.scb.scbbillingandcollection.auth.data.models.LoginRequest
import com.scb.scbbillingandcollection.auth.data.models.LoginResponse
import com.scb.scbbillingandcollection.auth.data.models.VersionResponse
import com.scb.scbbillingandcollection.collect_bill.models.CansRequest
import com.scb.scbbillingandcollection.collect_bill.models.CollectBillRequest
import com.scb.scbbillingandcollection.collect_bill.models.CollectBillResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.WardsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiInterface {

    @GET("versionCheck.json")
    suspend fun getPoliceStations(@Query("version") version: String): VersionResponse

   @GET("getMasterWards.json")
    suspend fun getWards(): WardsResponse

    @POST("userLogin.json")
    suspend fun checkLogin(@Body request: LoginRequest): LoginResponse

    @POST("getConsumersList.json")
    suspend fun generateBillList(@Body request: CansRequest): ConsumerListResponse

    @POST("viewBill.json")
    suspend fun viewBill(@Body request: ViewBillRequest): ViewBillResponse

    @POST("generateBill.json")
    suspend fun generateBill(@Body request: GenerateBillRequest): GenerateBillResponse

    @POST("collectBill.json")
    suspend fun collectBill(@Body request: CollectBillRequest): CollectBillResponse

}