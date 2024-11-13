package com.scb.scbbillingandcollection.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blankj.utilcode.util.ToastUtils
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.collect_bill.models.CollectionRequest
import com.scb.scbbillingandcollection.collect_bill.models.GetCollection
import com.scb.scbbillingandcollection.collect_bill.models.UpdateSCB
import com.scb.scbbillingandcollection.core.retrofit.ApiInterface
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.databinding.FragmentReportDetailsBinding
import com.scb.scbbillingandcollection.generate_bill.data.repository.GenerateBillRepositoryImpl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReportDetailsFragment : Fragment() {

    private lateinit var binding: FragmentReportDetailsBinding

    @Inject
    lateinit var repImpl: GenerateBillRepositoryImpl

    private val args:ReportDetailsFragmentArgs by navArgs()
    private val adapter = ReportDetailsAdapter{

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentReportDetailsBinding.inflate(layoutInflater)

        lifecycleScope.launch {
            getCollectionDetails()
        }
        return binding.root
    }

    private suspend fun getCollectionDetails() {

        val response = repImpl.getCollectionData(args.request)
        when (response) {
            is Resource.Success -> {
                binding.rvReportDetails.adapter = adapter
                if (response.value.receipts != null && response.value.receipts.isNotEmpty()) {
                    adapter.submitList(response.value.receipts)
                } else {
                    adapter.submitList(emptyList())
                    ToastUtils.showShort("no Details Found")
                }
            }

            is Resource.Failure -> {
                ToastUtils.showShort(response.errorBody.toString())
            }

            else -> {}
        }

    }

}