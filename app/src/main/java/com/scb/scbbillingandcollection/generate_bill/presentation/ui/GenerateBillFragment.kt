package com.scb.scbbillingandcollection.generate_bill.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.observerSharedFlow
import com.scb.scbbillingandcollection.core.extensions.showCustomToast
import com.scb.scbbillingandcollection.databinding.FragmentGenerateBillBinding
import com.scb.scbbillingandcollection.generate_bill.presentation.viewmodel.GenerateBillViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GenerateBillFragment : Fragment() {

    private var _binding: FragmentGenerateBillBinding? = null
    private val binding get() = _binding!!
    private val billViewModel: GenerateBillViewModel by navGraphViewModels(R.id.main_nav_graph) { defaultViewModelProviderFactory }

    private val args: GenerateBillFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentGenerateBillBinding.inflate(layoutInflater, container, false)
        initObservers()

        binding.generateBill.clickWithDebounce {
            billViewModel.dispatch(GenerateBillViewModel.BillActions.GenerateBill(args.request))
        }
        binding.apply {
            demandText.text = args.request.current_month_demand.toString()
            serviceChargesText.text = args.charges.toString()
            arrearsText.text = args.request.arrear.toString()
//            klLabelText.text = args.request.rebate_amt.toString()
            netText.text = args.request.net_amount.toString()
            generateBill.isEnabled = true
        }
        return binding.root
    }

    private fun initObservers() {


        observerSharedFlow(billViewModel.generateBillResponse) {
            it.data?.let {
                showCustomToast(R.drawable.ic_check_green, title = it)
                findNavController().navigate(
                    GenerateBillFragmentDirections.actionGenerateFragmentToGenerateCanListFragment(
                        true
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