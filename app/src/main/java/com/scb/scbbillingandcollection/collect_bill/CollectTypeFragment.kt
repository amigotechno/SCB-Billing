package com.scb.scbbillingandcollection.collect_bill

import android.app.DatePickerDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.collect_bill.models.CollectBillRequest
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.millisToDate
import com.scb.scbbillingandcollection.core.extensions.observerSharedFlow
import com.scb.scbbillingandcollection.core.extensions.showCustomToast
import com.scb.scbbillingandcollection.databinding.FragmentCollectTypeBinding
import com.scb.scbbillingandcollection.generate_bill.presentation.viewmodel.GenerateBillViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar


@AndroidEntryPoint
class CollectTypeFragment : Fragment() {
    private var _binding: FragmentCollectTypeBinding? = null
    private val binding get() = _binding!!
    private lateinit var selectedDate: Calendar // To store the selected date
    private val billViewModel: GenerateBillViewModel by navGraphViewModels(R.id.main_nav_graph) { defaultViewModelProviderFactory }

    private var selectedItem = ""

    private val args: CollectTypeFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectTypeBinding.inflate(layoutInflater, container, false)
        ArrayAdapter.createFromResource(
            requireContext(), R.array.collect_types, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.collectType.adapter = adapter
        }
        selectedDate = Calendar.getInstance()

        binding.balanceAmount.setText(args.customerResponse.payable_amount)

        binding.collectType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
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

        binding.amount.addTextChangedListener {
            if (it?.trim()?.isNotEmpty() == true) {
                val double = args.customerResponse.payable_amount?.toDouble()
                    ?.minus(it.toString().toDouble())
                binding.balanceAmount.setText(double.toString())
            } else {
                binding.balanceAmount.setText(args.customerResponse.payable_amount)
            }

        }

        binding.collectBtn.clickWithDebounce {
            validations()
        }

        return binding.root
    }

    private fun initObservers() {
        observerSharedFlow(billViewModel.collectBillResponse) {
            it.data?.let {
                showCustomToast(title = it)
                setData()
                findNavController().navigate(CollectTypeFragmentDirections.actionCollectTypeFragmentToGenerateCanListFragment(false))
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
                   val request =  CollectBillRequest(
                        can_id = args.customerResponse.id.toString()?:"",
                        collect_type = selectedItem,
                        amount = amount.text.toString(),
                        cheque_no = chequeNo.text.toString(),
                        cheque_bank = chequeBank.text.toString(),
                        cheque_date = chequeDate.text.toString(),
                        cheque_branch = chequeBranch.text.toString()
                    )
                    billViewModel.dispatch(GenerateBillViewModel.BillActions.CollectBill(request))

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
                   val request = CollectBillRequest(
                        can_id = args.customerResponse.id.toString()?:"",
                        collect_type = selectedItem,
                        amount = amount.text.toString(),
                        dd_no = ddNo.text.toString(),
                        dd_bank = ddBank.text.toString(),
                        dd_date = ddDate.text.toString(),
                        dd_branch = ddBranch.text.toString()
                    )
                    billViewModel.dispatch(GenerateBillViewModel.BillActions.CollectBill(request))
                }
            } else {
                //dp api call
                val request = CollectBillRequest(
                    can_id = args.customerResponse.id.toString()?:"",
                    collect_type = selectedItem,
                    amount = amount.text.toString()
                )
                billViewModel.dispatch(GenerateBillViewModel.BillActions.CollectBill(request))
            }
        }

    }

    private fun setData() {
        val intent = Intent()
        intent.setAction("com.pinelabs.masterapp.SERVER")
        intent.setPackage("com.pinelabs.masterapp")
        requireActivity().bindService(intent, printConnection, AppCompatActivity.BIND_AUTO_CREATE)
    }


    private val printConnection: ServiceConnection = object : ServiceConnection {
        var mServerMessenger: Messenger? = null
        var isBound = false

        //
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mServerMessenger = Messenger(service)
            isBound = true
            val message: Message = Message.obtain(null, 1001)
            val data = Bundle()
            val finalObject = JSONObject()
            val headerObject = JSONObject()
            val detailObject = JSONObject()
            try {
                headerObject.put("ApplicationId", "c375e49b009d4ecabbef7c7898ca9664")
                headerObject.put("UserId", "1001609")
                headerObject.put("MethodId", "1002")
                headerObject.put("VersionNo", "1.0")
                detailObject.put("PrintRefNo", "123446779")
                detailObject.put("SavePrintData", true)
                val arryData = JSONArray()
                val printData = JSONObject()
                printData.put("PrintDataType", 0)
                printData.put("PrinterWidth", 24)

                val billText = StringBuilder()

                val nextLine = "\n"
                billText.append("Secunderabad Containment")
                billText.append(nextLine)
                billText.append("Bheema Residency")
                billText.append(nextLine)
                val billEntries =
                    arrayOf(arrayOf("Date", millisToDate(System.currentTimeMillis())),
                        arrayOf("Receipt No", "123456"),
                        arrayOf("Cashier", "Sridhar"),
                        arrayOf("Total Amount", args.customerResponse.payable_amount),
                        arrayOf("Paid Amount", binding.amount.text.toString()),
                        arrayOf("Balance Amount", binding.balanceAmount.text.toString(), arrayOf("Payment Mode", selectedItem)))

                for (entry in billEntries) {
                    billText.append(String.format("%-10s%10s\n", entry[0], entry[1]))
                }
                billText.append(nextLine)
                billText.append("--------------------")
                billText.append(nextLine)
                billText.append("--------------------")
                billText.append(nextLine)
                billText.append(nextLine)

                printData.put("DataToPrint",billText)
                printData.put("IsCenterAligned", false)
                printData.put("ImagePath", android.R.attr.path)
                printData.put("ImageData", "")
                arryData.put(printData)
                detailObject.put("Data", arryData)
                finalObject.put("Header", headerObject)
                finalObject.put("Detail", detailObject)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            data.putString("MASTERAPPREQUEST", finalObject.toString())
            message.data = data
            try {
                message.replyTo = Messenger(IncomingHandler())
                mServerMessenger!!.send(message)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mServerMessenger = null
            isBound = false
        }
    }


    private class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            val bundle: Bundle = msg.getData()
            val value =
                bundle.getString("MASTERAPPRESPONSE") // process the response Json as required.
            Log.e("Tagresponse", value!!)
            bundle.clear()
            try {
                val `object` = JSONObject(value)
                val response = `object`.getString("Response")
                Log.e("JSON respons", "status-- $response")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            Log.e("Tagresponse", value!!)
        }
    }
}