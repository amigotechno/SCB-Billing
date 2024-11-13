package com.scb.scbbillingandcollection.dashboard

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.blankj.utilcode.util.ToastUtils
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.collect_bill.models.CollectionRequest
import com.scb.scbbillingandcollection.collect_bill.models.GetCollection
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.databinding.FragmentReportsBinding
import com.scb.scbbillingandcollection.generate_bill.data.repository.GenerateBillRepositoryImpl
import com.scb.scbbillingandcollection.generate_bill.presentation.viewmodel.GenerateBillViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject


@AndroidEntryPoint
class ReportsFragment : Fragment() {

    private lateinit var binding: FragmentReportsBinding
    private val billViewModel: GenerateBillViewModel by navGraphViewModels(R.id.main_nav_graph) { defaultViewModelProviderFactory }

    @Inject
    lateinit var repImpl: GenerateBillRepositoryImpl

    private val adapter = ReportsAdapter{
        findNavController().navigate(ReportsFragmentDirections.actionReportsFragmentToReportDetailsFragment(
            CollectionRequest(binding.start.text.toString(),it.collect_type)
        ))
    }

    var mYear = 0
    var mMonth = 0
    var mDay = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentReportsBinding.inflate(layoutInflater, container, false)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//        val currentDate: String = dateFormat.format(Date())

        binding.start.setOnClickListener {
            val c: Calendar = Calendar.getInstance()
            mYear = c.get(Calendar.YEAR)
            mMonth = c.get(Calendar.MONTH)
            mDay = c.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(
                requireActivity(),
                DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    var monthString: String = (monthOfYear + 1).toString()
                    if (monthString.length == 1) {
                        monthString = "0$monthString"
                    }
                    var day: String = (dayOfMonth).toString()
                    if (day.length == 1) {
                        day = "0$day"
                    }
                    binding.start.setText(year.toString() + "-" + monthString + "-" + day)
                    billViewModel.dateSelect = year.toString() + "-" + monthString + "-" + day
                    lifecycleScope.launch {
                        getReports();
                    }
                },
                mYear,
                mMonth,
                mDay
            )
            datePickerDialog.show()
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        }
        lifecycleScope.launch {
            getReports();
        }
    }

    private suspend fun getReports() {
        Log.d("TAG", "getReports: "+binding.start.text)
        val response = repImpl.getReports(GetCollection(billViewModel.dateSelect))
        when (response) {
            is Resource.Success -> {

                binding.rvTransactions.adapter = adapter
                if (response.value?.receipts != null && response.value.receipts.isNotEmpty()) {
                    adapter.submitList(response.value.receipts)
                } else {
                    adapter.submitList(emptyList())
                    ToastUtils.showShort("no Transactions Found")
                }

            }

            is Resource.Failure -> {
                ToastUtils.showShort(response.errorBody.toString())
            }

            else -> {}
        }
    }

}