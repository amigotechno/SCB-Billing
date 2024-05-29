package com.scb.scbbillingandcollection.collect_bill

import android.app.DatePickerDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
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
import com.google.gson.Gson
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.collect_bill.models.CollectBillRequest
import com.scb.scbbillingandcollection.core.base.AppPreferences
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.millisToDate
import com.scb.scbbillingandcollection.core.extensions.millisToTime
import com.scb.scbbillingandcollection.core.extensions.observerSharedFlow
import com.scb.scbbillingandcollection.core.extensions.showCustomToast
import com.scb.scbbillingandcollection.core.utils.Constants
import com.scb.scbbillingandcollection.databinding.FragmentCollectTypeBinding
import com.scb.scbbillingandcollection.generate_bill.presentation.viewmodel.GenerateBillViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Calendar
import javax.inject.Inject


@AndroidEntryPoint
class CollectTypeFragment : Fragment() {
    private var _binding: FragmentCollectTypeBinding? = null
    private val binding get() = _binding!!
    private lateinit var selectedDate: Calendar // To store the selected date
    private val billViewModel: GenerateBillViewModel by navGraphViewModels(R.id.main_nav_graph) { defaultViewModelProviderFactory }

    private var selectedItem = ""
    private var receiptNumber = ""

    private val args: CollectTypeFragmentArgs by navArgs()

    @Inject
    lateinit var appPreferences: AppPreferences

    lateinit var hexString: StringBuilder

    var isCard = false


    inner class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            val bundle: Bundle = msg.data
            val value =
                bundle.getString("MASTERAPPRESPONSE") // process the response Json as required.
            Log.e("Tagresponse", value!!)
            bundle.clear()
            try {
                val `object` = JSONObject(value)
                val response = `object`.getString("Response")
                val header = `object`.getString("Header")
                Log.e("JSON respons", "status-- $response")
                findNavController().navigate(
                    CollectTypeFragmentDirections.actionCollectTypeFragmentToGenerateCanListFragment(
                        false
                    )
                )
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            Log.e("Tagresponse", value)
        }
    }

    inner class PaymentHandler : Handler() {
        override fun handleMessage(msg: Message) {
            val bundle: Bundle = msg.data
            val value =
                bundle.getString("MASTERAPPRESPONSE") // process the response Json as required.
            Log.e("Tagresponse", value!!)
            bundle.clear()
            try {
                val `object` = JSONObject(value)
                val response = `object`.getString("Response")
                val header = `object`.getString("Header")
                Log.e("JSON respons", "status-- $response")
                val obj = JSONObject(response)

                if (obj.getString("ResponseMsg").equals("APPROVED")) {
                    doApiCall()
                }
                showCustomToast(title = obj.getString("ResponseMsg").toString())
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            Log.e("Tagresponse", value)
        }
    }

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
                        isCard = false
                        binding.details.isVisible = false
                    }

                    "Cheque" -> {
                        isCard = false
                        binding.details.isVisible = true
                        binding.chequeDetails.isVisible = true
                        binding.ddDetails.isVisible = false
                    }

                    "DD" -> {
                        isCard = false
                        binding.details.isVisible = true
                        binding.chequeDetails.isVisible = false
                        binding.ddDetails.isVisible = true
                    }

                    "Card" -> {
                        isCard = true
                        binding.details.isVisible = false
                    }

                    "QR" -> {
                        isCard = false
                        binding.details.isVisible = false
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
            if (selectedItem == "Card") {
                getPayment()
            } else if (selectedItem == "QR") {
                getQRPayment()
            } else {
                validations()
            }
        }


        // Load the drawable image as a byte array
        val imageBytes = getDrawableAsBytes(requireContext(), R.drawable.logo)

        // Convert the byte array to hexadecimal format
        hexString = StringBuilder()
        imageBytes.forEach {
            hexString.append(String.format("%02X", it))
        }


        return binding.root
    }

    private fun doApiCall() {
        val request = CollectBillRequest(
            can_id = args.customerResponse.id.toString(),
            collect_type = selectedItem,
            amount = binding.amount.text.toString()
        )
        billViewModel.dispatch(GenerateBillViewModel.BillActions.CollectBill(request))

    }

    private fun initObservers() {
        observerSharedFlow(billViewModel.collectBillResponse) {
            it.data?.let {
                receiptNumber = it
                showCustomToast(title = it)
                setData()
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
            } else if (amount.text.isEmpty() || amount.text.toString() == "0") {
                showCustomToast(title = "Enter Valid Amount")
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
                    val request = CollectBillRequest(
                        can_id = args.customerResponse.id.toString(),
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
                        can_id = args.customerResponse.id.toString(),
                        collect_type = selectedItem,
                        amount = amount.text.toString(),
                        dd_no = ddNo.text.toString(),
                        dd_bank = ddBank.text.toString(),
                        dd_date = ddDate.text.toString(),
                        dd_branch = ddBranch.text.toString()
                    )
                    billViewModel.dispatch(GenerateBillViewModel.BillActions.CollectBill(request))
                }
            } else if (selectedItem == "QR") {
                findNavController().navigate(
                    CollectTypeFragmentDirections.actionCollectTypeFragmentToQrWebFragment(
                        "https://scb.amigotechno.in/mobile-web/get-qr?can_id=" + args.customerResponse.id.toString() + "&amount=" + binding.amount.text.toString()
                    )
                )
            } else {
                //dp api call
                val request = CollectBillRequest(
                    can_id = args.customerResponse.id.toString(),
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

    private fun getPayment() {
        val intent = Intent()
        intent.setAction("com.pinelabs.masterapp.SERVER")
        intent.setPackage("com.pinelabs.masterapp")
        requireActivity().bindService(intent, paymentConnection, AppCompatActivity.BIND_AUTO_CREATE)
    }

    private fun getQRPayment() {
        val intent = Intent()
        intent.setAction("com.pinelabs.masterapp.SERVER")
        intent.setPackage("com.pinelabs.masterapp")
        requireActivity().bindService(intent, qrConnection, AppCompatActivity.BIND_AUTO_CREATE)
    }

    private fun getDrawableAsBytes(context: Context, drawableId: Int): ByteArray {
        val resources: Resources = context.resources
        val inputStream = resources.openRawResource(drawableId)
        val outputStream = ByteArrayOutputStream()

        try {
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return outputStream.toByteArray()
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
//                headerObject.put("ApplicationId", "c375e49b009d4ecabbef7c7898ca9664")
                headerObject.put("ApplicationId", Constants.PROD_APP_ID)
                headerObject.put("UserId", "1001609")
                headerObject.put("MethodId", "1002")
                headerObject.put("VersionNo", "1.0")
                detailObject.put("PrintRefNo", "123446779")
                detailObject.put("SavePrintData", true)
                val arrayData = JSONArray()


                val scbName = JSONObject()
                scbName.put("PrintDataType", 0)
                scbName.put("PrinterWidth", 28)
                scbName.put("DataToPrint", " SCB Water Collection \nCourt Compound Secunderabad")
                scbName.put("IsCenterAligned", true)
                scbName.put("ImagePath", "")
                scbName.put("ImageData", "")

                val dateTime = JSONObject()
                dateTime.put("PrintDataType", 0)
                dateTime.put("PrinterWidth", 28)
                dateTime.put(
                    "DataToPrint", formatText(
                        28,
                        "Date:" + millisToDate(System.currentTimeMillis()),
                        "Time:" + millisToTime(System.currentTimeMillis())
                    )
                )
                dateTime.put("IsCenterAligned", false)
                dateTime.put("ImagePath", "")
                dateTime.put("ImageData", "")

                val saleName = JSONObject()
                saleName.put("PrintDataType", 0)
                saleName.put("PrinterWidth", 24)
                saleName.put("DataToPrint", " Collection ")
                saleName.put("IsCenterAligned", true)
                saleName.put("ImagePath", "")
                saleName.put("ImageData", "")

                val paidBy = JSONObject()
                paidBy.put("PrintDataType", 0)
                paidBy.put("PrinterWidth", 28)
                paidBy.put("DataToPrint", formatText(28, "Paid By:", selectedItem))
                paidBy.put("IsCenterAligned", false)
                paidBy.put("ImagePath", "")
                paidBy.put("ImageData", "")

                val baseAmount = JSONObject()
                baseAmount.put("PrintDataType", 0)
                baseAmount.put("PrinterWidth", 24)
                baseAmount.put(
                    "DataToPrint",
                    formatText(24, "Paid Amt:", "Rs, " + binding.amount.text.toString())
                )
                baseAmount.put("IsCenterAligned", false)
                baseAmount.put("ImagePath", "")
                baseAmount.put("ImageData", "")

                val notRequired = JSONObject()
                notRequired.put("PrintDataType", 0)
                notRequired.put("PrinterWidth", 28)
                notRequired.put("DataToPrint", "Signature Not required")
                notRequired.put("IsCenterAligned", true)
                notRequired.put("ImagePath", "")
                notRequired.put("ImageData", "")

                val imageData = JSONObject()
                imageData.put("PrintDataType", 2)
                imageData.put("PrinterWidth", 24)
                imageData.put("DataToPrint", "")
                imageData.put("IsCenterAligned", true)
                imageData.put("ImagePath", "")
                imageData.put("ImageData", hexString)

                val receiptNo = JSONObject()
                receiptNo.put("PrintDataType", 0)
                receiptNo.put("PrinterWidth", 28)
                receiptNo.put("DataToPrint", formatText(28, "Receipt No :", receiptNumber))
                receiptNo.put("IsCenterAligned", false)
                receiptNo.put("ImagePath", "")
                receiptNo.put("ImageData", "")

                val ucnNo = JSONObject()
                ucnNo.put("PrintDataType", 0)
                ucnNo.put("PrinterWidth", 28)
                ucnNo.put(
                    "DataToPrint",
                    formatText(28, "UCN No :", args.customerResponse.can_number.toString())
                )
                ucnNo.put("IsCenterAligned", false)
                ucnNo.put("ImagePath", "")
                ucnNo.put("ImageData", "")

                val ownerName = JSONObject()
                ownerName.put("PrintDataType", 0)
                ownerName.put("PrinterWidth", 28)
                if (args.customerResponse.consumer_name.toString().length > 14) {
                    ownerName.put(
                        "DataToPrint",
                        "Owner Name : " + args.customerResponse.consumer_name.toString()
                    )
                } else {
                    ownerName.put(
                        "DataToPrint", formatText(
                            28, "Owner Name :", args.customerResponse.consumer_name.toString()
                        )
                    )
                }
                ownerName.put("IsCenterAligned", false)
                ownerName.put("ImagePath", "")
                ownerName.put("ImageData", "")
                val plotNo = JSONObject()
                plotNo.put("PrintDataType", 0)
                plotNo.put("PrinterWidth", 28)
                if (args.customerResponse.plot_no.toString().length > 14) {
                    plotNo.put(
                        "DataToPrint", "Plot No : " + args.customerResponse.plot_no.toString()
                    )
                } else {
                    plotNo.put(
                        "DataToPrint",
                        formatText(28, "Plot No :", args.customerResponse.plot_no.toString())
                    )
                }
                plotNo.put("IsCenterAligned", false)
                plotNo.put("ImagePath", "")
                plotNo.put("ImageData", "")

                val location = JSONObject()
                location.put("PrintDataType", 0)
                location.put("PrinterWidth", 28)
                if (args.customerResponse.location.toString().length > 14) {
                    location.put(
                        "DataToPrint", "Location : " + args.customerResponse.location.toString()
                    )
                } else {
                    location.put(
                        "DataToPrint",
                        formatText(28, "Location :", args.customerResponse.location.toString())
                    )
                }
                location.put("IsCenterAligned", false)
                location.put("ImagePath", "")
                location.put("ImageData", "")

                val mobileNo = JSONObject()
                mobileNo.put("PrintDataType", 0)
                mobileNo.put("PrinterWidth", 28)
                mobileNo.put(
                    "DataToPrint",
                    formatText(28, "Mobile No :", args.customerResponse.phone_no.toString())
                )
                mobileNo.put("IsCenterAligned", false)
                mobileNo.put("ImagePath", "")
                mobileNo.put("ImageData", "")

                val totalAmount = JSONObject()
                totalAmount.put("PrintDataType", 0)
                totalAmount.put("PrinterWidth", 28)
                totalAmount.put(
                    "DataToPrint", formatText(
                        28, "Total Amount :", args.customerResponse.payable_amount.toString()
                    )
                )
                totalAmount.put("IsCenterAligned", false)
                totalAmount.put("ImagePath", "")
                totalAmount.put("ImageData", "")

                val paidAmount = JSONObject()
                paidAmount.put("PrintDataType", 0)
                paidAmount.put("PrinterWidth", 28)
                paidAmount.put(
                    "DataToPrint", formatText(28, "Paid AMt :", binding.amount.text.toString())
                )
                paidAmount.put("IsCenterAligned", false)
                paidAmount.put("ImagePath", "")
                paidAmount.put("ImageData", "")

                val balanceAmount = JSONObject()
                balanceAmount.put("PrintDataType", 0)
                balanceAmount.put("PrinterWidth", 28)
                balanceAmount.put(
                    "DataToPrint",
                    formatText(28, "Balance Amt :", binding.balanceAmount.text.toString())
                )
                balanceAmount.put("IsCenterAligned", false)
                balanceAmount.put("ImagePath", "")
                balanceAmount.put("ImageData", "")


                val gap = JSONObject()
                gap.put("PrintDataType", 0)
                gap.put("PrinterWidth", 24)
                gap.put("DataToPrint", " -------------------- \n ")
                gap.put("IsCenterAligned", true)
                gap.put("ImagePath", "")
                gap.put("ImageData", "")

                arrayData.put(scbName)
                arrayData.put(dateTime)
                arrayData.put(saleName)
                arrayData.put(paidBy)
                arrayData.put(baseAmount)
                arrayData.put(notRequired)
                arrayData.put(imageData)
                arrayData.put(receiptNo)
                arrayData.put(ucnNo)
                arrayData.put(ownerName)
                arrayData.put(plotNo)
                arrayData.put(location)
                arrayData.put(mobileNo)
                arrayData.put(totalAmount)
                arrayData.put(balanceAmount)
                arrayData.put(gap)

                detailObject.put("Data", arrayData)
                finalObject.put("Header", headerObject)
                finalObject.put("Detail", detailObject)
                data.putString("MASTERAPPREQUEST", finalObject.toString())
                message.data = data
                message.replyTo = Messenger(IncomingHandler())
                mServerMessenger!!.send(message)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mServerMessenger = null
            isBound = false
        }
    }
    private val paymentConnection: ServiceConnection = object : ServiceConnection {
        var mPaymentMessenger: Messenger? = null
        var isPaymentBound = false

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mPaymentMessenger = Messenger(service)
            isPaymentBound = true
            try {
                val message: Message = Message.obtain(null, 1001)
                val data = Bundle()
                val gson = Gson()
                val map = hashMapOf(
//                    "ApplicationId" to "c375e49b009d4ecabbef7c7898ca9664",
                    "ApplicationId" to Constants.PROD_APP_ID,
                    "UserId" to "1001609",
                    "MethodId" to "1001",
                    "VersionNo" to "1.0"
                )

                val map1 = hashMapOf(
                    "TransactionType" to "4001",
                    "BillingRefNo" to args.customerResponse.can_number,
                    "PaymentAmount" to binding.amount.text.toString().toDouble() * 100
                )

                val h1 = hashMapOf(
                    "Header" to map, "Detail" to map1
                )
                data.putString("MASTERAPPREQUEST", gson.toJson(h1))
                Log.d("TAG", "onServiceConnected: " + gson.toJson(h1))
                message.data = data
                message.replyTo = Messenger(PaymentHandler())
                mPaymentMessenger!!.send(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mPaymentMessenger = null
            isPaymentBound = false
        }
    }
    private val qrConnection: ServiceConnection = object : ServiceConnection {
        var mQRMessenger: Messenger? = null
        var isQRBound = false

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mQRMessenger = Messenger(service)
            isQRBound = true
            try {
                val message: Message = Message.obtain(null, 1001)
                val data = Bundle()
                val gson = Gson()
                val map = hashMapOf(
                    "ApplicationId" to Constants.PROD_APP_ID,
//                    "ApplicationId" to "4a22e5c0956840da8dbea1d1bc5292b4",
                    "UserId" to "1001609",
                    "MethodId" to "1001",
                    "VersionNo" to "1.0"
                )

                val map1 = hashMapOf(
                    "TransactionType" to "5120",
                    "BillingRefNo" to args.customerResponse.can_number,
                    "PaymentAmount" to  binding.amount.text.toString().toDouble() * 100
                )

                val h1 = hashMapOf(
                    "Header" to map, "Detail" to map1
                )
                data.putString("MASTERAPPREQUEST", gson.toJson(h1))
                Log.d("TAG", "onServiceConnected: " + gson.toJson(h1))
                message.data = data
                message.replyTo = Messenger(PaymentHandler())
                mQRMessenger!!.send(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mQRMessenger = null
            isQRBound = false
        }
    }

    fun formatText(receiptWidth: Int, leftText: String, rightText: String): String {
        val leftWidth = receiptWidth / 2
        val rightWidth = receiptWidth - leftWidth
        val formattedLeftText = leftText.take(leftWidth)
        val formattedRightText = rightText.takeLast(rightWidth)
        return "$formattedLeftText${" ".repeat(receiptWidth - formattedLeftText.length - formattedRightText.length)}$formattedRightText"
    }

}