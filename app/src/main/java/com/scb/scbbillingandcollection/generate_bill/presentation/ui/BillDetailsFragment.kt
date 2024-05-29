package com.scb.scbbillingandcollection.generate_bill.presentation.ui

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
import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.ToastUtils
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.collect_bill.models.GetCanId
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.millisToDate
import com.scb.scbbillingandcollection.core.extensions.millisToTime
import com.scb.scbbillingandcollection.core.extensions.observerSharedFlow
import com.scb.scbbillingandcollection.core.extensions.showCustomToast
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.core.utils.Constants
import com.scb.scbbillingandcollection.databinding.FragmentBillDetailsBinding
import com.scb.scbbillingandcollection.generate_bill.data.models.DemandAndCollectBill
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.models.ViewBillRequest
import com.scb.scbbillingandcollection.generate_bill.data.repository.GenerateBillRepositoryImpl
import com.scb.scbbillingandcollection.generate_bill.presentation.viewmodel.GenerateBillViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BillDetailsFragment : Fragment() {

    private var _binding: FragmentBillDetailsBinding? = null
    private val binding get() = _binding!!
    private val billViewModel: GenerateBillViewModel by navGraphViewModels(R.id.main_nav_graph) { defaultViewModelProviderFactory }

    private val args: BillDetailsFragmentArgs by navArgs()

    lateinit var hexString: StringBuilder
    private var selectedItem = ""

    private var printData: DemandAndCollectBill? = null

    @Inject
    lateinit var repImpl: GenerateBillRepositoryImpl

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

        val imageBytes = getDrawableAsBytes(requireContext(), R.drawable.logo)
        hexString = StringBuilder()
        imageBytes.forEach {
            hexString.append(String.format("%02X", it))
        }

        binding.apply {
            categoryText.text = args.customerResponse.category
            meterNoText.text = args.customerResponse.can_number
            nameText.text = args.customerResponse.consumer_name
            plotText.text = args.customerResponse.plot_no
            pipeSizeTxt.text = args.customerResponse.pipe_size
            addressText.text = args.customerResponse.location
        }

        binding.meterStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                selectedItem = parent.getItemAtPosition(position).toString()
                if (selectedItem.equals("Metered")) {
                    binding.presentReadingLayout.visibility = View.VISIBLE
                } else {
                    binding.presentReadingLayout.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }

        binding.viewBill.clickWithDebounce {
            KeyboardUtils.hideSoftInput(requireActivity())
            if (selectedItem == "") {
                showCustomToast(title = "Select Meter Status")
            } else if (selectedItem.equals("Metered") && binding.presentReading.text.isEmpty()) {
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
        lifecycleScope.launch {
            getPrintData()
        }

        binding.demandBill.clickWithDebounce {

            if (printData != null) {
                setData()
            } else {
                ToastUtils.showShort("Something Went Wrong")
            }
        }

        binding.receiptBill.clickWithDebounce {
            if (printData != null) {
                getReceipt()
            } else {
                ToastUtils.showShort("Something Went Wrong")
            }
        }

        return binding.root
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
                headerObject.put("ApplicationId", Constants.PROD_APP_ID)
//                headerObject.put("ApplicationId", "4a22e5c0956840da8dbea1d1bc5292b4")
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

                val noticeTitle = JSONObject()
                noticeTitle.put("PrintDataType", 0)
                noticeTitle.put("PrinterWidth", 28)
                noticeTitle.put("DataToPrint", " Water Bill Cum \n Demand Notice")
                noticeTitle.put("IsCenterAligned", true)
                noticeTitle.put("ImagePath", "")
                noticeTitle.put("ImageData", "")

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


                val imageData = JSONObject()
                imageData.put("PrintDataType", 2)
                imageData.put("PrinterWidth", 24)
                imageData.put("DataToPrint", "")
                imageData.put("IsCenterAligned", true)
                imageData.put("ImagePath", "")
                imageData.put("ImageData", hexString)

                val ucnNo = JSONObject()
                ucnNo.put("PrintDataType", 0)
                ucnNo.put("PrinterWidth", 28)
                ucnNo.put(
                    "DataToPrint",
                    formatText(28, "UCN No :", printData?.ucn_details?.can_number.toString())
                )
                ucnNo.put("IsCenterAligned", false)
                ucnNo.put("ImagePath", "")
                ucnNo.put("ImageData", "")

                val ownerName = JSONObject()
                ownerName.put("PrintDataType", 0)
                ownerName.put("PrinterWidth", 28)
                if (printData?.ucn_details?.consumer_name.toString().length > 14) {
                    ownerName.put(
                        "DataToPrint", "Name : " + printData?.ucn_details?.consumer_name.toString()
                    )
                } else {
                    ownerName.put(
                        "DataToPrint",
                        formatText(28, "Name :", printData?.ucn_details?.consumer_name.toString())
                    )
                }
                ownerName.put("IsCenterAligned", false)
                ownerName.put("ImagePath", "")
                ownerName.put("ImageData", "")
                val plotNo = JSONObject()
                plotNo.put("PrintDataType", 0)
                plotNo.put("PrinterWidth", 28)
                if (printData?.ucn_details?.plot_no.toString().length > 14) {
                    plotNo.put(
                        "DataToPrint", "Plot No : " + printData?.ucn_details?.plot_no.toString()
                    )
                } else {
                    plotNo.put(
                        "DataToPrint",
                        formatText(28, "Plot No :", printData?.ucn_details?.plot_no.toString())
                    )
                }
                plotNo.put("IsCenterAligned", false)
                plotNo.put("ImagePath", "")
                plotNo.put("ImageData", "")

                val location = JSONObject()
                location.put("PrintDataType", 0)
                location.put("PrinterWidth", 28)
                if (printData?.ucn_details?.location.toString().length > 14) {
                    location.put(
                        "DataToPrint", "Address : " + printData?.ucn_details?.location.toString()
                    )
                } else {
                    location.put(
                        "DataToPrint",
                        formatText(28, "Address :", printData?.ucn_details?.location.toString())
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

                val currentDemand = JSONObject()
                currentDemand.put("PrintDataType", 0)
                currentDemand.put("PrinterWidth", 28)
                currentDemand.put(
                    "DataToPrint",
                    formatText(
                        28,
                        "Current Demand :",
                        printData?.bill?.current_month_demand.toString()
                    )
                )
                currentDemand.put("IsCenterAligned", false)
                currentDemand.put("ImagePath", "")
                currentDemand.put("ImageData", "")

                val currentMonth = JSONObject()
                currentMonth.put("PrintDataType", 0)
                currentMonth.put("PrinterWidth", 28)
                currentMonth.put(
                    "DataToPrint",
                    formatText(
                        28,
                        "Curr Mth :",
                        convertDateToMonthAbbreviation(printData?.bill?.billdate.toString())
                    )
                )
                currentMonth.put("IsCenterAligned", false)
                currentMonth.put("ImagePath", "")
                currentMonth.put("ImageData", "")

                val arrearAmount = JSONObject()
                arrearAmount.put("PrintDataType", 0)
                arrearAmount.put("PrinterWidth", 28)
                arrearAmount.put(
                    "DataToPrint",
                    formatText(28, "Arrears :", printData?.bill?.arrear.toString())
                )
                arrearAmount.put("IsCenterAligned", false)
                arrearAmount.put("ImagePath", "")
                arrearAmount.put("ImageData", "")

                val billAmount = JSONObject()
                billAmount.put("PrintDataType", 0)
                billAmount.put("PrinterWidth", 28)
                billAmount.put(
                    "DataToPrint",
                    formatText(28, "Bill Amt :", printData?.bill?.net_amount.toString())
                )
                billAmount.put("IsCenterAligned", false)
                billAmount.put("ImagePath", "")
                billAmount.put("ImageData", "")


                val gap = JSONObject()
                gap.put("PrintDataType", 0)
                gap.put("PrinterWidth", 24)
                gap.put("DataToPrint", " -------------------- \n ")
                gap.put("IsCenterAligned", true)
                gap.put("ImagePath", "")
                gap.put("ImageData", "")

                arrayData.put(scbName)
                arrayData.put(noticeTitle)
                arrayData.put(dateTime)
//                arrayData.put(imageData)
                arrayData.put(ucnNo)
                arrayData.put(ownerName)
                arrayData.put(plotNo)
                arrayData.put(location)
                arrayData.put(mobileNo)
                arrayData.put(currentDemand)
                arrayData.put(currentMonth)
                arrayData.put(arrearAmount)
                arrayData.put(billAmount)
                arrayData.put(gap)

                detailObject.put("Data", arrayData)
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


    fun convertDateToMonthAbbreviation(dateString: String): String {
        val inputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val date = inputDateFormat.parse(dateString)
        val outputDateFormat = SimpleDateFormat("MMM", Locale.US)
        return outputDateFormat.format(date).toUpperCase(Locale.US)
    }

    fun formatText(receiptWidth: Int, leftText: String, rightText: String): String {
        val leftWidth = receiptWidth / 2
        val rightWidth = receiptWidth - leftWidth
        val formattedLeftText = leftText.take(leftWidth)
        val formattedRightText = rightText.takeLast(rightWidth)
        return "$formattedLeftText${" ".repeat(receiptWidth - formattedLeftText.length - formattedRightText.length)}$formattedRightText"
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
                    ""
                )
                findNavController().navigate(
                    BillDetailsFragmentDirections.actionBillDetailsFragmentToGenerateBillFragment(
                        request,
                        it.service_charges.toString(), args.customerResponse
                    )
                )
            }
            it.error?.let {
                showCustomToast(R.drawable.ic_error_warning, title = it)
            }
        }
    }

    private fun getReceipt() {
        val intent = Intent()
        intent.setAction("com.pinelabs.masterapp.SERVER")
        intent.setPackage("com.pinelabs.masterapp")
        requireActivity().bindService(intent, receiptConnection, AppCompatActivity.BIND_AUTO_CREATE)
    }

    private val receiptConnection: ServiceConnection = object : ServiceConnection {
        var mServerReceiptMessenger: Messenger? = null
        var isBoundReceipt = false

        //
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mServerReceiptMessenger = Messenger(service)
            isBoundReceipt = true
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
                        "Date:" + printData?.receipt?.receipt_date.toString(),
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
                paidBy.put(
                    "DataToPrint",
                    formatText(28, "Paid By:", printData?.receipt?.collect_type.toString())
                )
                paidBy.put("IsCenterAligned", false)
                paidBy.put("ImagePath", "")
                paidBy.put("ImageData", "")

                val baseAmount = JSONObject()
                baseAmount.put("PrintDataType", 0)
                baseAmount.put("PrinterWidth", 24)
                baseAmount.put(
                    "DataToPrint",
                    formatText(24, "Paid Amt:", "Rs, " + printData?.receipt?.amount.toString())
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
                receiptNo.put(
                    "DataToPrint",
                    formatText(28, "Receipt No :", printData?.receipt?.receipt_no.toString())
                )
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
                    "DataToPrint",
                    formatText(28, "Paid Amt :", printData?.receipt?.amount.toString())
                )
                paidAmount.put("IsCenterAligned", false)
                paidAmount.put("ImagePath", "")
                paidAmount.put("ImageData", "")

                val amount = (printData!!.bill!!.net_amount.toDouble()) - (printData!!.bill!!.paid_amount.toDouble())
                val balanceAmount = JSONObject()
                balanceAmount.put("PrintDataType", 0)
                balanceAmount.put("PrinterWidth", 28)
                balanceAmount.put(
                    "DataToPrint",
                    formatText(28, "Balance Amt :", amount.toString())
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
                mServerReceiptMessenger!!.send(message)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mServerReceiptMessenger = null
            isBoundReceipt = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private suspend fun getPrintData() {
        val response = repImpl.printData(GetCanId(args.customerResponse.id.toString()))
        when (response) {
            is Resource.Success -> {
                printData = response.value
            }

            is Resource.Failure -> {
                ToastUtils.showShort(response.errorBody.toString())
            }

            else -> {}
        }
    }

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
                Log.e("JSON respons", "status-- $response")

            } catch (e: JSONException) {
                e.printStackTrace()
            }
            Log.e("Tagresponse", value)
        }
    }
}