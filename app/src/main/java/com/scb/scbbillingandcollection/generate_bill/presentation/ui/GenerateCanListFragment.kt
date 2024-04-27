package com.scb.scbbillingandcollection.generate_bill.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.keka.xhr.core.app.di.CustomDialogQualifier
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.collect_bill.models.CansRequest
import com.scb.scbbillingandcollection.collect_bill.models.GetCan
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.dismissCompact
import com.scb.scbbillingandcollection.core.extensions.observerState
import com.scb.scbbillingandcollection.core.extensions.showCompact
import com.scb.scbbillingandcollection.core.extensions.showCustomToast
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.databinding.FragmentGenerateCanListBinding
import com.scb.scbbillingandcollection.generate_bill.data.repository.GenerateBillRepository
import com.scb.scbbillingandcollection.generate_bill.presentation.adapter.ConsumersListAdapter
import com.scb.scbbillingandcollection.generate_bill.presentation.viewmodel.GenerateBillViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GenerateCanListFragment : Fragment() {

    private var _binding: FragmentGenerateCanListBinding? = null
    private val binding get() = _binding!!

    private val billViewModel: GenerateBillViewModel by navGraphViewModels(R.id.main_nav_graph) { defaultViewModelProviderFactory }

    private val args: GenerateCanListFragmentArgs by navArgs()
    private val adapter = ConsumersListAdapter {

        if (args.fromGenerate) {
            findNavController().navigate(
                GenerateCanListFragmentDirections.actionGenerateCanListFragmentToBillDetailsFragment(
                    it
                )
            )

        } else {
            findNavController().navigate(
                GenerateCanListFragmentDirections.actionGenerateCanListFragmentToCollectBillDetailsFragment(
                    it
                )
            )
        }
    }

    @Inject
    @CustomDialogQualifier
    lateinit var dialog: AlertDialog

    @Inject
    lateinit var repository: GenerateBillRepository

    var selectedItem: Pair<String, String>? = null
    var selectedBeat: Pair<String, String>? = null

    var wardsList = ArrayList<Pair<String, String>>()
    var beatsList = ArrayList<Pair<String, String>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentGenerateCanListBinding.inflate(layoutInflater, container, false)

        binding.rvConsumers.adapter = adapter
        initObservers()
//        dialog.showCompact()
        binding.search.addTextChangedListener {
            billViewModel.dispatch(GenerateBillViewModel.BillActions.SearchQuery(it.toString()))
        }

        binding.searchCan.clickWithDebounce {
            if (binding.ucnNo.text.toString().length < 4) {
                showCustomToast(title = "Enter Valid UCN Number")
            } else {

                lifecycleScope.launch {
                    getUCNInfo(GetCan(binding.ucnNo.text.toString().trim()))
                }
            }
        }

        binding.serachBtn.clickWithDebounce {
            if (selectedItem != null && selectedItem?.first != "0") {
                val request = CansRequest(selectedItem?.first ?: "", selectedBeat?.first ?: "")
                dialog.showCompact()
                billViewModel.dispatch(GenerateBillViewModel.BillActions.GetCansList(request))
            } else {
                showCustomToast(title = "Select Ward and Search")
            }

        }
        binding.wardNo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                selectedItem = wardsList[position]
                billViewModel.spinnerPosition = position
                billViewModel.beatPosition = 0
                selectedBeat = null
                dialog.showCompact()
                billViewModel.dispatch(GenerateBillViewModel.BillActions.GetBeatsList(selectedItem?.second.toString()))
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }

        binding.beatNo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                dialog.dismissCompact()
                selectedBeat = beatsList[position]
                billViewModel.beatPosition = position
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }

        return binding.root
    }

    private fun initObservers() {
        observerState(billViewModel.consumersList) {
            it.data?.let {
                if (it.isEmpty()) {
                    showCustomToast(title = "No Data Found")
                }
                binding.searchView.isVisible = true
                adapter.submitList(it)
                dialog.dismissCompact()
            }
            it.error?.let {
                showCustomToast(R.drawable.ic_error_warning, title = it)
                dialog.dismissCompact()
            }
        }
        observerState(billViewModel.wardsList) {
            it.data?.let {
                if (it.isEmpty()) {
                    showCustomToast(title = "No Wards Found")
                }
                wardsList = it
                val finalList = arrayListOf<String>()

                for (i in it) {
                    finalList.add(i.second)
                }

                val adapter =
                    ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_item, finalList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.wardNo.adapter = adapter
//                binding.wardNo.setSelection(billViewModel.spinnerPosition)
            }
            it.error?.let {
                showCustomToast(R.drawable.ic_error_warning, title = it)
            }
        }

        observerState(billViewModel.beatsList) {
            it.data?.let {
                if (it.isEmpty()) {
                    showCustomToast(title = "No Beats Found")
                }
                beatsList = it
                val finalList = arrayListOf<String>()

                for (i in it) {
                    finalList.add(i.second)
                }

                val adapter =
                    ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_item, finalList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.beatNo.adapter = adapter
//                binding.beatNo.setSelection(billViewModel.beatPosition)
            }
            it.error?.let {
                showCustomToast(R.drawable.ic_error_warning, title = it)
            }
        }
    }

    suspend fun getUCNInfo(ucn: GetCan) {
        when (val response = repository.searchUCN(ucn)) {
            is Resource.Success -> {
                if (response.value.ucn_details != null) {
                    if (args.fromGenerate) {
                        findNavController().navigate(
                            GenerateCanListFragmentDirections.actionGenerateCanListFragmentToBillDetailsFragment(
                                response.value.ucn_details
                            )
                        )

                    } else {
                        findNavController().navigate(
                            GenerateCanListFragmentDirections.actionGenerateCanListFragmentToCollectBillDetailsFragment(
                                response.value.ucn_details
                            )
                        )
                    }
                } else {
                    showCustomToast(title = "Invalid UCN")
                }
            }

            is Resource.Failure -> {
                showCustomToast(title = response.errorBody.toString())
            }

            else -> {}
        }
    }

}

