package com.scb.scbbillingandcollection.collect_bill

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.collect_bill.models.CollectBillRequest
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.observerSharedFlow
import com.scb.scbbillingandcollection.core.extensions.showCustomToast
import com.scb.scbbillingandcollection.databinding.FragmentCollectTypeBinding
import com.scb.scbbillingandcollection.generate_bill.presentation.viewmodel.GenerateBillViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class CollectTypeFragment : Fragment() {
    private var _binding: FragmentCollectTypeBinding? = null
    private val binding get() = _binding!!
    private lateinit var selectedDate: Calendar // To store the selected date
    private val billViewModel: GenerateBillViewModel by navGraphViewModels(R.id.main_nav_graph) { defaultViewModelProviderFactory }

    private var selectedItem = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectTypeBinding.inflate(layoutInflater, container, false)
        // Inflate the layout for this fragment
        ArrayAdapter.createFromResource(
            requireContext(), R.array.collect_types, android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            binding.collectType.adapter = adapter
        }
        selectedDate = Calendar.getInstance()

        binding.collectType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                // Item selected from the spinner
                selectedItem = parent.getItemAtPosition(position).toString()
                when (selectedItem) {
                    "Cash" -> {
                        binding.details.isVisible = false
                    }

                    "Cheque" -> {
                        binding.details.isVisible = true
                        binding.chequeDetails.isVisible = true
                        binding.ddDetails.isVisible = false
                    }

                    "DD" -> {
                        binding.details.isVisible = true
                        binding.chequeDetails.isVisible = false
                        binding.ddDetails.isVisible = true
                    }


                }
            }


            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }
        initObservers()
        binding.chequeDate.setOnClickListener {
            showDatePickerDialog(binding.chequeDate)
        }

        binding.ddDate.setOnClickListener {
            showDatePickerDialog(binding.ddDate)
        }

        return binding.root
    }

    private fun initObservers() {
        observerSharedFlow(billViewModel.collectBillResponse) {
            it.data?.let {
                showCustomToast(title = it)
                binding.collectBtn.isVisible = false
            }
            it.error?.let {
                showCustomToast(title = it)
            }
        }
    }


    private fun showDatePickerDialog(edittextView: EditText) {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            DatePickerDialog.OnDateSetListener { view: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, monthOfYear)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // You can perform any action here with the selected date
                val selectedDateString = "$dayOfMonth-${monthOfYear + 1}-$year"
                edittextView.setText(selectedDateString)
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()

        binding.collectBtn.clickWithDebounce {
            validations()
        }
    }

    fun validations() {
        binding.apply {
            if (selectedItem == "") {
                showCustomToast(title = "Select Type of Mode")
            } else if (amount.text.isEmpty()) {
                showCustomToast(title = "Enter Amount")
            } else if (selectedItem == "Cheque") {
                if (chequeNo.text.isEmpty()) {
                    showCustomToast(title = "Enter Cheque Number")
                } else if (chequeDate.text.isEmpty()) {
                    showCustomToast(title = "Select Cheque Date")
                } else if (chequeBank.text.isEmpty()) {
                    showCustomToast(title = "Enter Cheque Bank")
                } else if (chequeBranch.text.isEmpty()) {
                    showCustomToast(title = "Enter Cheque Branch")
                } else {
                    //do api call
                    CollectBillRequest(
                        can_id = "",
                        collect_type = selectedItem,
                        amount = amount.text.toString(),
                        cheque_no = chequeNo.text.toString(),
                        cheque_bank = chequeBank.text.toString(),
                        cheque_date = chequeDate.text.toString(),
                        cheque_branch = chequeBranch.text.toString()
                    )

                }
            } else if (selectedItem == "DD") {
                if (ddNo.text.isEmpty()) {
                    showCustomToast(title = "Enter DD Number")
                } else if (ddDate.text.isEmpty()) {
                    showCustomToast(title = "Select DD Date")
                } else if (ddBank.text.isEmpty()) {
                    showCustomToast(title = "Enter DD Bank")
                } else if (ddBranch.text.isEmpty()) {
                    showCustomToast(title = "Enter DD Branch")
                } else {
                    //do api call
                    CollectBillRequest(
                        can_id = "",
                        collect_type = selectedItem,
                        amount = amount.text.toString(),
                        dd_no = ddNo.text.toString(),
                        dd_bank = ddBank.text.toString(),
                        dd_date = ddDate.text.toString(),
                        dd_branch = ddBranch.text.toString()
                    )
                }
            } else {
                //dp api call
                CollectBillRequest(
                    can_id = "",
                    collect_type = selectedItem,
                    amount = amount.text.toString()
                )
            }
        }

    }
}