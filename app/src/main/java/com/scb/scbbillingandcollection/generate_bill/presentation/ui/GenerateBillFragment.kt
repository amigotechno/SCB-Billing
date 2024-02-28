package com.scb.scbbillingandcollection.generate_bill.presentation.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.observerSharedFlow
import com.scb.scbbillingandcollection.core.extensions.observerState
import com.scb.scbbillingandcollection.core.extensions.showCustomToast
import com.scb.scbbillingandcollection.databinding.FragmentGenerateBillBinding
import com.scb.scbbillingandcollection.databinding.FragmentLoginBinding
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillRequest
import com.scb.scbbillingandcollection.generate_bill.presentation.viewmodel.GenerateBillViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GenerateBillFragment : Fragment() {

    private var _binding: FragmentGenerateBillBinding? = null
    private val binding get() = _binding!!
    private val billViewModel : GenerateBillViewModel by navGraphViewModels(R.id.generateBillGraph) {defaultViewModelProviderFactory}

    private val args: GenerateBillFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentGenerateBillBinding.inflate(layoutInflater,container,false)
        initObservers()

        billViewModel.dispatch(GenerateBillViewModel.BillActions.ViewBill(ViewBillRequest(args.canId)))

        binding.generateBill.clickWithDebounce {

        }
        return binding.root
    }

    private fun initObservers() {
        observerState(billViewModel.viewBillResponse){
            it.data?.let {
                binding.apply {
                    demandText.text = it.current_month_demand.toString()
                    serviceChargesText.text = it.service_charges.toString()
                    arrearsText.text = it.arrear.toString()
                    klLabelText.text = it.rebate_amt.toString()
                    netText.text = it.net_amount.toString()
                    generateBill.isEnabled = true
                }
            }
            it.error?.let {
                binding.generateBill.isEnabled = false
                showCustomToast(R.drawable.ic_error_warning, title = it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}