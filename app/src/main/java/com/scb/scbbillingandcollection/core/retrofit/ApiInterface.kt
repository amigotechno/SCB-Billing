package com.scb.scbbillingandcollection.core.retrofit

import com.scb.scbbillingandcollection.generate_bill.data.models.ConsumerListResponse
import com.scb.scbbillingandcollection.auth.data.models.LoginRequest
import com.scb.scbbillingandcollection.auth.data.models.LoginResponse
import com.scb.scbbillingandcollection.auth.data.models.VersionResponse
import com.scb.scbbillingandcollection.collect_bill.models.CansRequest
import com.scb.scbbillingandcollection.collect_bill.models.CollectBillRequest
import com.scb.scbbillingandcollection.collect_bill.models.CollectBillResponse
import com.scb.scbbillingandcollection.collect_bill.models.CollectionDetails
import com.scb.scbbillingandcollection.collect_bill.models.CollectionModel
import com.scb.scbbillingandcollection.collect_bill.models.CollectionRequest
import com.scb.scbbillingandcollection.collect_bill.models.GetCan
import com.scb.scbbillingandcollection.collect_bill.models.GetCanId
import com.scb.scbbillingandcollection.collect_bill.models.GetCollection
import com.scb.scbbillingandcollection.collect_bill.models.UpdateSCB
import com.scb.scbbillingandcollection.generate_bill.data.models.BeatsResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.DemandAndCollectBill
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.UCNDetails
import com.scb.scbbillingandcollection.generate_bill.data.models.UpdateScbResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.WardsResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiInterface {

    @GET("versionCheck.json")
    suspend fun getPoliceStations(@Query("version") version: String): VersionResponse

   @GET("getMasterWards.json")
    suspend fun getWards(): WardsResponse

   @GET("getMasterBeatCodes.json")
    suspend fun getBeatCodes(@Query("ward") ward:String): BeatsResponse

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

    @POST("getByUCN.json")
    suspend fun getUcnInfo(@Body request: GetCan): UCNDetails

    @POST("getUCNBillReceipt.json")
    suspend fun printData(@Body request: GetCanId): DemandAndCollectBill

    @POST("getCollections.json")
    suspend fun getCollections(@Body request: GetCollection): CollectionModel

    @POST("updateScbNo.json")
    suspend fun updateSCBNo(@Body request: UpdateSCB): UpdateScbResponse

    @POST("getCollectionData.json")
    suspend fun getCollectionData(@Body request: CollectionRequest): CollectionDetails

}