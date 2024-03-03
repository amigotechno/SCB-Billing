package com.scb.scbbillingandcollection.generate_bill.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.observerSharedFlow
import com.scb.scbbillingandcollection.core.extensions.showCustomToast
import com.scb.scbbillingandcollection.databinding.FragmentBillDetailsBinding
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillRequest
import com.scb.scbbillingandcollection.generate_bill.presentation.viewmodel.GenerateBillViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BillDetailsFragment : Fragment() {

    private var _binding: FragmentBillDetailsBinding? = null
    private val binding get() = _binding!!
    private val billViewModel: GenerateBillViewModel by navGraphViewModels(R.id.main_nav_graph) { defaultViewModelProviderFactory }

    private val args: BillDetailsFragmentArgs by navArgs()

    private var selectedItem = ""
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentBillDetailsBinding.inflate(layoutInflater, container, false)
        initObservers()

        ArrayAdapter.createFromResource(
            requireContext(), R.array.meter_status, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.meterStatus.adapter = adapter
        }
        binding.apply {
            categoryText.text = args.customerResponse.category
            meterNoText.text = args.customerResponse.can_number
            pipeSizeTxt.text = args.customerResponse.pipe_size
            addressText.text = args.customerResponse.location
        }

        binding.meterStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                selectedItem = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }

        binding.viewBill.clickWithDebounce {
            if (selectedItem == "") {
                showCustomToast(title = "Select Meter Status")
            } else if (binding.presentReading.text.isEmpty()) {
                showCustomToast(title = "Enter Reading")
            } else {
                billViewModel.dispatch(
                    GenerateBillViewModel.BillActions.ViewBill(
                        ViewBillRequest(
                            args.customerResponse.id.toString(),
                            binding.presentReading.text.toString(),
                            selectedItem
                        )
                    )
                )
            }
        }

        return binding.root
    }

    private fun initObservers() {
        observerSharedFlow(billViewModel.viewBillResponse) {
            it.data?.let {
                val request = GenerateBillRequest(
                    args.customerResponse.id.toString(),
                    binding.presentReading.text.toString(),
                    selectedItem,
                    it.current_month_demand.toString(),
                    it.rebate_amt.toString(),
                    it.arrear.toString(),
                    it.net_amount.toString(),
                )
                findNavController().navigate(
                    BillDetailsFragmentDirections.actionBillDetailsFragmentToGenerateBillFragment(
                        request,
                        it.service_charges.toString()
                    )
                )
            }
            it.error?.let {
                showCustomToast(R.drawable.ic_error_warning, title = it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}