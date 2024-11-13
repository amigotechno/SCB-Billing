package com.scb.scbbillingandcollection.generate_bill.data.repository

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
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.generate_bill.data.models.BeatsResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.ConsumerListResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.DemandAndCollectBill
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.UCNDetails
import com.scb.scbbillingandcollection.generate_bill.data.models.UpdateScbResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.WardsResponse
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody

interface GenerateBillRepository {

    fun getConsumersList(request: CansRequest): Flow<Resource<ConsumerListResponse>>
    fun viewBill(request : ViewBillRequest): Flow<Resource<ViewBillResponse>>
    suspend fun getWards(): Resource<WardsResponse>
    suspend fun getBeatCodes(wardNo:String): Resource<BeatsResponse>
    fun generateBill(request : GenerateBillRequest): Flow<Resource<GenerateBillResponse>>
    fun collectBill(request : CollectBillRequest): Flow<Resource<CollectBillResponse>>
    suspend fun searchUCN(request : GetCan): Resource<UCNDetails>
    suspend fun printData(request: GetCanId): Resource<DemandAndCollectBill>
    suspend fun getReports(request: GetCollection): Resource<CollectionModel>
    suspend fun updateSCBNo(request: UpdateSCB): Resource<UpdateScbResponse>
    suspend fun getCollectionData(request: CollectionRequest): Resource<CollectionDetails>
}