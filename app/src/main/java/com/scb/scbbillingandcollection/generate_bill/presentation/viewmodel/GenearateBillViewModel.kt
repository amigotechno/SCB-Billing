package com.scb.scbbillingandcollection.generate_bill.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.scb.scbbillingandcollection.core.base.ActionViewModel
import com.scb.scbbillingandcollection.core.base.BaseAction
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.generate_bill.data.models.Consumers
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillResponse
import com.scb.scbbillingandcollection.generate_bill.data.repository.GenerateBillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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

    private var _viewBillResponse = MutableStateFlow(BillState.ViewBill())
    val viewBillResponse = _viewBillResponse.asStateFlow()

    var finalList: List<Consumers?>? = null

    init {
        generateBillList()
    }

    private fun generateBillList() {
        viewModelScope.launch {
            repository.getConsumersList().collectLatest { response ->
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
                            if (error == 0){
                                _viewBillResponse.update {
                                    it.copy(data = response.value.amounts, error = null)
                                }
                            }else{
                                _viewBillResponse.update {
                                    it.copy(data = null, error = response.value.message)
                                }
                            }
                        }

                    }

                    is Resource.Failure -> {
                        _viewBillResponse.update {
                            it.copy(data = null, error = response.errorBody.toString())
                        }
                    }

                    else -> {}
                }
            }
        }
    }
    private fun viewBill(request: GenerateBillRequest) {
        viewModelScope.launch {
            repository.generateBill(request).collectLatest { response ->
                when (response) {
                    is Resource.Success -> {
                        response.value.apply {
                            if (error == 0){
                                _viewBillResponse.update {
                                    it.copy(data = response.value.amounts, error = null)
                                }
                            }else{
                                _viewBillResponse.update {
                                    it.copy(data = null, error = response.value.message)
                                }
                            }
                        }

                    }

                    is Resource.Failure -> {
                        _viewBillResponse.update {
                            it.copy(data = null, error = response.errorBody.toString())
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun searchQuery(query: String) {
        viewModelScope.launch {
            val filteredList = finalList?.filter { it?.can_number?.contains(query, ignoreCase = true) == true }
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
                    viewBill(action.request)
                }
            }
        }

    }


    sealed class BillActions : BaseAction {
        data class SearchQuery(val query: String) : BillActions()
        data class ViewBill(val request: ViewBillRequest) : BillActions()
        data class GenerateBill(val request: GenerateBillRequest) : BillActions()

    }

    sealed class BillState {
        data class ConsumerList(val data: List<Consumers?>? = null, val error: String? = null)
        data class ViewBill(val data: ViewBillResponse.Amounts? = null, val error: String? = null)
    }
}