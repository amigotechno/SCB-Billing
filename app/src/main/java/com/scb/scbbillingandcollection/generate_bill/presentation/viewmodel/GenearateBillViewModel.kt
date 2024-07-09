package com.scb.scbbillingandcollection.generate_bill.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.scb.scbbillingandcollection.collect_bill.models.CansRequest
import com.scb.scbbillingandcollection.collect_bill.models.CollectBillRequest
import com.scb.scbbillingandcollection.core.base.ActionViewModel
import com.scb.scbbillingandcollection.core.base.BaseAction
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.generate_bill.data.models.Consumers
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillResponse
import com.scb.scbbillingandcollection.generate_bill.data.repository.GenerateBillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenerateBillViewModel @Inject constructor(private val repository: GenerateBillRepository) :
    ActionViewModel<GenerateBillViewModel.BillActions>() {

    private var _consumersList = MutableStateFlow(BillState.ConsumerList())
    val consumersList = _consumersList.asStateFlow()

    private var _wardsList = MutableStateFlow(BillState.WardsData())
    val wardsList = _wardsList.asStateFlow()

    private var _beatsList = MutableStateFlow(BillState.BeatsData())
    val beatsList = _beatsList.asStateFlow()

    private var _viewBillResponse = MutableSharedFlow<BillState.ViewBill>()
    val viewBillResponse = _viewBillResponse.asSharedFlow()

    private var _generateBillResponse = MutableSharedFlow<BillState.GenerateBillResponse>()
    val generateBillResponse = _generateBillResponse.asSharedFlow()

    private var _collectBillResponse = MutableSharedFlow<BillState.CollectBillResponse>()
    val collectBillResponse = _collectBillResponse.asSharedFlow()

    var finalList: List<Consumers?>? = null

    var spinnerPosition = 0
    var beatPosition = 0

    init {
        viewModelScope.launch {
            getWards()
        }
    }

    private suspend fun getWards() {
        when (val response = repository.getWards()) {
            is Resource.Success -> {

                val gson = Gson()
                val mapType = object : TypeToken<Map<String, String>>() {}.type
                val wards: Map<String, String> =
                    gson.fromJson(response.value.wards.toString(), mapType)

                val wardsList = arrayListOf<Pair<String, String>>()
                wardsList.add(Pair("0", "Ward"))
                wards.forEach { (wardKey, wardNo) ->
                    wardsList.add(Pair(wardKey, wardNo))
                }
                _wardsList.update {
                    it.copy(data = wardsList, error = null)
                }
            }

            is Resource.Failure -> {

            }

            else -> {}
        }

    }

    private suspend fun getBeats(wardNo: String) {
        when (val response = repository.getBeatCodes(wardNo)) {
            is Resource.Success -> {

                val gson = Gson()
                val mapType = object : TypeToken<Map<String, String>>() {}.type
                val wards: Map<String, String> =
                    gson.fromJson(response.value.beat_codes.toString(), mapType)

                val wardsList = arrayListOf<Pair<String, String>>()
                wardsList.add(Pair("0", "Beat Code"))
                wards.forEach { (wardKey, wardNo) ->
                    wardsList.add(Pair(wardKey, wardNo))
                }
                _beatsList.update {
                    it.copy(data = wardsList, error = null)
                }
            }

            is Resource.Failure -> {

            }

            else -> {}
        }

    }


    private fun generateBillList(request: CansRequest) {
        viewModelScope.launch {
            repository.getConsumersList(request).collectLatest { response ->
                when (response) {
                    is Resource.Success -> {
                        _consumersList.update {
                            it.copy(data = response.value.consumers_list, error = null)
                        }
                        finalList = response.value.consumers_list
                    }

                    is Resource.Failure -> {
                        _consumersList.update {
                            it.copy(data = null, error = response.errorBody.toString())
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun viewBill(request: ViewBillRequest) {
        viewModelScope.launch {
            repository.viewBill(request).collectLatest { response ->
                when (response) {
                    is Resource.Success -> {
                        response.value.apply {
                            if (error == 0) {
                                _viewBillResponse.emit(
                                    BillState.ViewBill(
                                        data = response.value.amounts,
                                        error = null
                                    )
                                )
                            } else {
                                _viewBillResponse.emit(
                                    BillState.ViewBill(
                                        data = null,
                                        error = response.value.message
                                    )
                                )
                            }
                        }

                    }

                    is Resource.Failure -> {
                        _viewBillResponse.emit(
                            BillState.ViewBill(
                                data = null,
                                error = response.errorBody.toString()
                            )
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    private fun generateBill(request: GenerateBillRequest) {
        viewModelScope.launch {
            repository.generateBill(request).collectLatest { response ->
                when (response) {
                    is Resource.Success -> {
                        response.value.apply {
                            if (error == "0") {
                                _generateBillResponse.emit(
                                    BillState.GenerateBillResponse(
                                        billNo = response.value.bill_number.toString(),
                                        billDate = response.value.bill_date,
                                        prevReading = response.value.previous_reading,
                                        prevReadingDate = response.value.previous_reading_date,
                                        presentReading = response.value.present_reading,
                                        presentReadingDate = response.value.present_reading_date,
                                        units = response.value.units,
                                        error = null
                                    )
                                )
                            } else {
                                _generateBillResponse.emit(
                                    BillState.GenerateBillResponse(
                                        billNo = null,
                                        billDate = null,
                                        error = response.value.message
                                    )
                                )
                            }
                        }

                    }

                    is Resource.Failure -> {
                        _generateBillResponse.emit(
                            BillState.GenerateBillResponse(
                                billNo = null,
                                billDate = null,
                                error = response.errorBody.toString()
                            )
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    private fun collectBill(request: CollectBillRequest) {
        viewModelScope.launch {
            repository.collectBill(request).collectLatest { response ->
                when (response) {
                    is Resource.Success -> {
                        response.value.apply {
                            if (error == "0") {
                                _collectBillResponse.emit(
                                    BillState.CollectBillResponse(
                                        data = response.value.receipt_no.toString(),
                                        error = null
                                    )
                                )
                            } else {
                                _collectBillResponse.emit(
                                    BillState.CollectBillResponse(
                                        data = null,
                                        error = response.value.message
                                    )
                                )
                            }
                        }

                    }

                    is Resource.Failure -> {
                        _collectBillResponse.emit(
                            BillState.CollectBillResponse(
                                data = null,
                                error = response.errorBody.toString()
                            )
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    private fun searchQuery(query: String) {
        viewModelScope.launch {
            val filteredList =
                finalList?.filter { it?.can_number?.contains(query, ignoreCase = true) == true }
            _consumersList.update {
                it.copy(filteredList)
            }

        }

    }

    override suspend fun bindActions() {
        actions.collect { action ->
            when (action) {
                is BillActions.SearchQuery -> {
                    searchQuery(action.query)
                }

                is BillActions.ViewBill -> {
                    viewBill(action.request)
                }

                is BillActions.GenerateBill -> {
                    generateBill(action.request)
                }

                is BillActions.CollectBill -> {
                    collectBill(action.request)
                }

                is BillActions.GetCansList -> {
                    generateBillList(action.request)
                }

                is BillActions.GetBeatsList -> {
                    getBeats(action.wardNo)
                }
            }
        }

    }


    sealed class BillActions : BaseAction {
        data class SearchQuery(val query: String) : BillActions()
        data class ViewBill(val request: ViewBillRequest) : BillActions()
        data class GenerateBill(val request: GenerateBillRequest) : BillActions()
        data class CollectBill(val request: CollectBillRequest) : BillActions()

        data class GetCansList(val request: CansRequest) : BillActions()
        data class GetBeatsList(val wardNo: String) : BillActions()

    }

    sealed class BillState {
        data class ConsumerList(val data: List<Consumers?>? = null, val error: String? = null)
        data class ViewBill(val data: ViewBillResponse.Amounts? = null, val error: String? = null)
        data class GenerateBillResponse(
            val billNo: String? = null,
            val billDate: String? = null,
            val prevReading: String? = null,
            val presentReading: String? = null,
            val prevReadingDate: String? = null,
            val presentReadingDate: String? = null,
            val units: String? = null,
            val error: String? = null
        )

        data class CollectBillResponse(val data: String? = null, val error: String? = null)
        data class WardsData(
            val data: ArrayList<Pair<String, String>>? = null,
            val error: String? = null
        )

        data class BeatsData(
            val data: ArrayList<Pair<String, String>>? = null,
            val error: String? = null
        )
    }
}