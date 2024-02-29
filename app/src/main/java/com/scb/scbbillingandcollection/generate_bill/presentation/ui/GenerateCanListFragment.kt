package com.scb.scbbillingandcollection.generate_bill.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.keka.xhr.core.app.di.CustomDialogQualifier
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.collect_bill.CollectBillDetailsFragment
import com.scb.scbbillingandcollection.core.extensions.dismissCompact
import com.scb.scbbillingandcollection.core.extensions.observerState
import com.scb.scbbillingandcollection.core.extensions.showCompact
import com.scb.scbbillingandcollection.core.extensions.showCustomToast
import com.scb.scbbillingandcollection.databinding.FragmentGenerateCanListBinding
import com.scb.scbbillingandcollection.generate_bill.presentation.adapter.ConsumersListAdapter
import com.scb.scbbillingandcollection.generate_bill.presentation.viewmodel.GenerateBillViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GenerateCanListFragment : Fragment() {

    private var _binding: FragmentGenerateCanListBinding? = null
    private val binding get() = _binding!!

    private val billViewModel : GenerateBillViewModel by navGraphViewModels(R.id.main_nav_graph) {defaultViewModelProviderFactory}

    private val args: GenerateCanListFragmentArgs by navArgs()
    private val adapter = ConsumersListAdapter{

        if (args.fromGenerate){
            findNavController().navigate(GenerateCanListFragmentDirections.actionGenerateCanListFragmentToGenerateBillFragment(it.can_number.toString()))

        }else{
            findNavController().navigate(GenerateCanListFragmentDirections.actionGenerateCanListFragmentToCollectBillDetailsFragment(it.can_number.toString()))
        }
          }

    @Inject
    @CustomDialogQualifier
    lateinit var dialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentGenerateCanListBinding.inflate(layoutInflater, container, false)

        binding.rvConsumers.adapter = adapter
        initObservers()
        dialog.showCompact()
        binding.search.addTextChangedListener {
            billViewModel.dispatch(GenerateBillViewModel.BillActions.SearchQuery(it.toString()))
        }
        return binding.root
    }

    private fun initObservers() {
        observerState(billViewModel.consumersList){
            it.data?.let {
                if (it.isEmpty()){
                    showCustomToast(title = "No Data Found")
                }
                adapter.submitList(it)
                dialog.dismissCompact()
            }
            it.error?.let {
                showCustomToast(R.drawable.ic_error_warning, title = it)
                dialog.dismissCompact()
            }
        }
    }

}

