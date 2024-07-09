package com.scb.scbbillingandcollection.generate_bill.data.repository

import com.scb.scbbillingandcollection.collect_bill.models.CansRequest
import com.scb.scbbillingandcollection.collect_bill.models.CollectBillRequest
import com.scb.scbbillingandcollection.collect_bill.models.CollectBillResponse
import com.scb.scbbillingandcollection.collect_bill.models.CollectionModel
import com.scb.scbbillingandcollection.collect_bill.models.GetCan
import com.scb.scbbillingandcollection.collect_bill.models.GetCanId
import com.scb.scbbillingandcollection.collect_bill.models.GetCollection
import com.scb.scbbillingandcollection.collect_bill.models.UpdateSCB
import com.scb.scbbillingandcollection.core.retrofit.ApiInterface
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.core.retrofit.SafeApiCall
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
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import javax.inject.Inject

class GenerateBillRepositoryImpl @Inject constructor(private val apiInterface: ApiInterface) : SafeApiCall,
    GenerateBillRepository {
    override fun getConsumersList(request: CansRequest): Flow<Resource<ConsumerListResponse>> = flow {
        val response = safeApiCall {
            apiInterface.generateBillList(request)
        }
        if (response is Resource.Success) {
            emit(response)
        } else {
            emit(response as Resource.Failure)
        }
    }
    override suspend fun getWards(): Resource<WardsResponse> {
        val response = safeApiCall {
            apiInterface.getWards()
        }
        return if (response is Resource.Success) {
            Resource.Success(response.value)
        } else response
    }
    override suspend fun getBeatCodes(wardNo:String): Resource<BeatsResponse> {
        val response = safeApiCall {
            apiInterface.getBeatCodes(wardNo)
        }
        return if (response is Resource.Success) {
            Resource.Success(response.value)
        } else response
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

    override fun collectBill(request: CollectBillRequest): Flow<Resource<CollectBillResponse>> = flow {
        val response = safeApiCall {
            apiInterface.collectBill(request)
        }
        if (response is Resource.Success) {
            emit(response)
        } else {
            emit(response as Resource.Failure)
        }
    }

    override suspend fun searchUCN(request: GetCan): Resource<UCNDetails> {
        val response = safeApiCall {
            apiInterface.getUcnInfo(request)
        }
        return if (response is Resource.Success) {
            Resource.Success(response.value)
        } else response
    }

    override suspend fun printData(request: GetCanId): Resource<DemandAndCollectBill> {
        val response = safeApiCall {
            apiInterface.printData(request)
        }
        return if (response is Resource.Success) {
            Resource.Success(response.value)
        } else response
    }

    override suspend fun getReports(request: GetCollection): Resource<CollectionModel> {
        val response = safeApiCall {
            apiInterface.getCollections(request)
        }
        return if (response is Resource.Success) {
            Resource.Success(response.value)
        } else response
    }

    override suspend fun updateSCBNo(request: UpdateSCB): Resource<UpdateScbResponse> {
        val response = safeApiCall {
            apiInterface.updateSCBNo(request)
        }
        return if (response is Resource.Success) {
            Resource.Success(response.value)
        } else response
    }
}