package com.scb.scbbillingandcollection.generate_bill.data.repository

import com.scb.scbbillingandcollection.collect_bill.models.CollectBillRequest
import com.scb.scbbillingandcollection.collect_bill.models.CollectBillResponse
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.generate_bill.data.models.ConsumerListResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillResponse
import kotlinx.coroutines.flow.Flow

interface GenerateBillRepository {

    fun getConsumersList(): Flow<Resource<ConsumerListResponse>>
    fun viewBill(request : ViewBillRequest): Flow<Resource<ViewBillResponse>>
    fun generateBill(request : GenerateBillRequest): Flow<Resource<GenerateBillResponse>>
    fun collectBill(request : CollectBillRequest): Flow<Resource<CollectBillResponse>>

}