package com.scb.scbbillingandcollection.generate_bill.data.repository

import com.scb.scbbillingandcollection.core.retrofit.ApiInterface
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.core.retrofit.SafeApiCall
import com.scb.scbbillingandcollection.generate_bill.data.models.ConsumerListResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillResponse
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GenerateBillRepositoryImpl @Inject constructor(private val apiInterface: ApiInterface) : SafeApiCall,
    GenerateBillRepository {
    override fun getConsumersList(): Flow<Resource<ConsumerListResponse>> = flow {
        val response = safeApiCall {
            apiInterface.generateBillList()
        }
        if (response is Resource.Success) {
            emit(response)
        } else {
            emit(response as Resource.Failure)
        }
    }

    override fun viewBill(request: ViewBillRequest): Flow<Resource<ViewBillResponse>> = flow{
        val response = safeApiCall {
            apiInterface.viewBill(request)
        }
        if (response is Resource.Success) {
            emit(response)
        } else {
            emit(response as Resource.Failure)
        }
    }

    override fun generateBill(request: GenerateBillRequest): Flow<Resource<GenerateBillResponse>> = flow{
        val response = safeApiCall {
            apiInterface.generateBill(request)
        }
        if (response is Resource.Success) {
            emit(response)
        } else {
            emit(response as Resource.Failure)
        }
    }
}