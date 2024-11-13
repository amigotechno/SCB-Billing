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
import androidx.core.view.isVisible
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

        if (args.customerResponse.is_fws == 1) {
            binding.fwsLayout.visibility = View.VISIBLE
        } else {
            binding.fwsLayout.visibility = View.GONE
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
        binding.updateSCB.clickWithDebounce {
            findNavController().navigate(
                BillDetailsFragmentDirections.actionBillDetailsFragmentToUpdateSCBFragment(
                    args.customerResponse.id.toString()
                )
            )
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

                val pipeSize = JSONObject()
                pipeSize.put("PrintDataType", 0)
                pipeSize.put("PrinterWidth", 28)
                pipeSize.put(
                    "DataToPrint",
                    formatText(28, "Pipe Size :", args.customerResponse.pipe_size.toString())
                )
                pipeSize.put("IsCenterAligned", false)
                pipeSize.put("ImagePath", "")
                pipeSize.put("ImageData", "")

                val category = JSONObject()
                category.put("PrintDataType", 0)
                category.put("PrinterWidth", 28)
                if (printData?.ucn_details?.category.toString().length > 14) {
                    category.put(
                        "DataToPrint", ("Category : " + printData?.ucn_details?.category)
                    )
                } else {
                    category.put(
                        "DataToPrint",
                        formatText(28, "Category:", printData?.ucn_details?.category?:"")
                    )
                }

                category.put("IsCenterAligned", false)
                category.put("ImagePath", "")
                category.put("ImageData", "")

                val currentDemand = JSONObject()
                currentDemand.put("PrintDataType", 0)
                currentDemand.put("PrinterWidth", 28)
                currentDemand.put(
                    "DataToPrint",
                    formatText(
                        28,
                        "Current Demand:",
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
                        printData?.ucn_details?.last_billed_date.toString()
                    )
                )
//                convertDateToMonthAbbreviation(printData?.bill?.billdate.toString()
                currentMonth.put("IsCenterAligned", false)
                currentMonth.put("ImagePath", "")
                currentMonth.put("ImageData", "")

                val billNo = JSONObject()
                billNo.put("PrintDataType", 0)
                billNo.put("PrinterWidth", 28)
                billNo.put(
                    "DataToPrint",
                    formatText(28, "Bill No :", printData?.bill?.bill_no.toString())
                )
                billNo.put("IsCenterAligned", false)
                billNo.put("ImagePath", "")
                billNo.put("ImageData", "")


                /// FWS

                val adhaarStatus = JSONObject()
                adhaarStatus.put("PrintDataType", 0)
                adhaarStatus.put("PrinterWidth", 28)
                adhaarStatus.put("DataToPrint", formatText(28, "Aadhaar Status:", printData?.ucn_details?.aadhar_status?:"" ))
                adhaarStatus.put("IsCenterAligned", false)
                adhaarStatus.put("ImagePath", "")
                adhaarStatus.put("ImageData", "")

                val meterStatus = JSONObject()
                meterStatus.put("PrintDataType", 0)
                meterStatus.put("PrinterWidth", 28)
                meterStatus.put("DataToPrint", formatText(28, "Meter Status:", printData?.ucn_details?.meter_status?:""))
                meterStatus.put("IsCenterAligned", false)
                meterStatus.put("ImagePath", "")
                meterStatus.put("ImageData", "")

                val prevReading = JSONObject()
                prevReading.put("PrintDataType", 0)
                prevReading.put("PrinterWidth", 28)
                prevReading.put(
                    "DataToPrint",
                    formatText(28, "Open :", printData?.bill?.previous_reading ?: "0")
                )
                prevReading.put("IsCenterAligned", false)
                prevReading.put("ImagePath", "")
                prevReading.put("ImageData", "")
                val prevReadingDate = JSONObject()
                prevReadingDate.put("PrintDataType", 0)
                prevReadingDate.put("PrinterWidth", 28)
                prevReadingDate.put(
                    "DataToPrint",
                    formatText(28, "From :", printData?.bill?.previousreading_date ?: "")
                )
                prevReadingDate.put("IsCenterAligned", false)
                prevReadingDate.put("ImagePath", "")
                prevReadingDate.put("ImageData", "")


                val currReading = JSONObject()
                currReading.put("PrintDataType", 0)
                currReading.put("PrinterWidth", 28)
                currReading.put(
                    "DataToPrint",
                    formatText(28, "Close :", printData?.bill?.present_reading ?: "0")
                )
                currReading.put("IsCenterAligned", false)
                currReading.put("ImagePath", "")
                currReading.put("ImageData", "")
                val currReadingDate = JSONObject()
                currReadingDate.put("PrintDataType", 0)
                currReadingDate.put("PrinterWidth", 28)
                currReadingDate.put(
                    "DataToPrint",
                    formatText(28, "To :", printData?.bill?.presentreading_date ?: "")
                )
                currReadingDate.put("IsCenterAligned", false)
                currReadingDate.put("ImagePath", "")
                currReadingDate.put("ImageData", "")


                val units = JSONObject()
                units.put("PrintDataType", 0)
                units.put("PrinterWidth", 28)
                units.put("DataToPrint", formatText(28, "Units (KL) :", printData?.bill?.units ?: "0"))
                units.put("IsCenterAligned", false)
                units.put("ImagePath", "")
                units.put("ImageData", "")

                //---------------------------------------------

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


                val fwsAmount = JSONObject()
                fwsAmount.put("PrintDataType", 0)
                fwsAmount.put("PrinterWidth", 28)
                fwsAmount.put(
                    "DataToPrint",
                    formatText(28, "20KL F.W.S :", printData?.bill?.rebate_amount.toString())
                )
                fwsAmount.put("IsCenterAligned", false)
                fwsAmount.put("ImagePath", "")
                fwsAmount.put("ImageData", "")

                val totalAmount = JSONObject()
                totalAmount.put("PrintDataType", 0)
                totalAmount.put("PrinterWidth", 28)
                totalAmount.put(
                    "DataToPrint", formatText(
                        28, "Total Amount :", printData?.bill?.total_amount.toString()
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
                    formatText(28, "Paid Amt :", printData?.bill?.paid_amount.toString())
                )
                paidAmount.put("IsCenterAligned", false)
                paidAmount.put("ImagePath", "")
                paidAmount.put("ImageData", "")


                val waterDemand = JSONObject()
                waterDemand.put("PrintDataType", 0)
                waterDemand.put("PrinterWidth", 28)
                waterDemand.put(
                    "DataToPrint",
                    formatText(28, "Water Demand :", printData?.bill?.fws_water_demand.toString())
                )
                waterDemand.put("IsCenterAligned", false)
                waterDemand.put("ImagePath", "")
                waterDemand.put("ImageData", "")

                val rebateAmt = JSONObject()
                rebateAmt.put("PrintDataType", 0)
                rebateAmt.put("PrinterWidth", 28)
                rebateAmt.put(
                    "DataToPrint",
                    formatText(28, "FWS Rebate :", printData?.bill?.fws_rebate.toString())
                )
                rebateAmt.put("IsCenterAligned", false)
                rebateAmt.put("ImagePath", "")
                rebateAmt.put("ImageData", "")

                val netDemand = JSONObject()
                netDemand.put("PrintDataType", 0)
                netDemand.put("PrinterWidth", 28)
                netDemand.put(
                    "DataToPrint",
                    formatText(28, "Net Demand(A) :", printData?.bill?.fws_net_demand.toString())
                )
                netDemand.put("IsCenterAligned", false)
                netDemand.put("ImagePath", "")
                netDemand.put("ImageData", "")

                val serviceCharges = JSONObject()
                serviceCharges.put("PrintDataType", 0)
                serviceCharges.put("PrinterWidth", 28)
                serviceCharges.put(
                    "DataToPrint",
                    formatText(28, "Service Amt(B) :", printData?.bill?.fws_service_charges.toString())
                )
                serviceCharges.put("IsCenterAligned", false)
                serviceCharges.put("ImagePath", "")
                serviceCharges.put("ImageData", "")

                val arrears = JSONObject()
                arrears.put("PrintDataType", 0)
                arrears.put("PrinterWidth", 28)
                arrears.put(
                    "DataToPrint",
                    formatText(28, "Arrears(C) :", printData?.bill?.fws_arrears.toString())
                )
                arrears.put("IsCenterAligned", false)
                arrears.put("ImagePath", "")
                arrears.put("ImageData", "")

                val totalBalAmount = JSONObject()
                totalBalAmount.put("PrintDataType", 0)
                totalBalAmount.put("PrinterWidth", 28)
                totalBalAmount.put(
                    "DataToPrint",
                    formatText(28, "Total Amt :", printData?.bill?.fws_total_payable_amount.toString())
                )
                totalBalAmount.put("IsCenterAligned", false)
                totalBalAmount.put("ImagePath", "")
                totalBalAmount.put("ImageData", "")



                val billAmount = JSONObject()
                billAmount.put("PrintDataType", 0)
                billAmount.put("PrinterWidth", 28)
                billAmount.put(
                    "DataToPrint",
                    formatText(28, "Balance Amt :", printData?.bill?.net_amount.toString())
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

                val gaps = JSONObject()
                gaps.put("PrintDataType", 0)
                gaps.put("PrinterWidth", 24)
                gaps.put("DataToPrint", " --------------------  ")
                gaps.put("IsCenterAligned", true)
                gaps.put("ImagePath", "")
                gaps.put("ImageData", "")

                val sd = JSONObject()
                sd.put("PrintDataType", 0)
                sd.put("PrinterWidth", 28)
                sd.put(
                    "DataToPrint",
                    formatText(
                        28,
                        "",
                        "Sd/-"
                    )
                )

                sd.put("IsCenterAligned", false)
                sd.put("ImagePath", "")
                sd.put("ImageData", "")

                val ceo = JSONObject()
                ceo.put("PrintDataType", 0)
                ceo.put("PrinterWidth", 28)
                ceo.put(
                    "DataToPrint",
                    formatText(
                        28,
                        "",
                        "For CEO, SCB"
                    )
                )

                ceo.put("IsCenterAligned", false)
                ceo.put("ImagePath", "")
                ceo.put("ImageData", "")

                val saveWater = JSONObject()
                saveWater.put("PrintDataType", 0)
                saveWater.put("PrinterWidth", 28)
                saveWater.put("DataToPrint", "SAVE WATER.WATER IS PRECIOUS")
                saveWater.put("IsCenterAligned", true)
                saveWater.put("ImagePath", "")
                saveWater.put("ImageData", "")

                val emptyGap = JSONObject()
                emptyGap.put("PrintDataType", 0)
                emptyGap.put("PrinterWidth", 24)
                emptyGap.put("DataToPrint", "  ")
                emptyGap.put("IsCenterAligned", true)
                emptyGap.put("ImagePath", "")
                emptyGap.put("ImageData", "")
                arrayData.put(scbName)
                arrayData.put(emptyGap)
                arrayData.put(noticeTitle)
                arrayData.put(emptyGap)
                arrayData.put(dateTime)
                arrayData.put(imageData)
                arrayData.put(emptyGap)
                arrayData.put(billNo)
                arrayData.put(emptyGap)
//                if (args.customerResponse.is_fws == 1) {
                    arrayData.put(adhaarStatus)
                    arrayData.put(emptyGap)
                    arrayData.put(meterStatus)
                    arrayData.put(emptyGap)
//                    arrayData.put(prevReading)
//                    arrayData.put(emptyGap)
//                    arrayData.put(currReading)
//                    arrayData.put(emptyGap)
                    arrayData.put(units)
                    arrayData.put(emptyGap)
                    arrayData.put(prevReadingDate)
                    arrayData.put(emptyGap)
                    arrayData.put(currReadingDate)
                    arrayData.put(emptyGap)
//                }
                arrayData.put(ucnNo)
                arrayData.put(emptyGap)
                arrayData.put(ownerName)
                arrayData.put(emptyGap)
                arrayData.put(plotNo)
                arrayData.put(emptyGap)
                arrayData.put(location)
                arrayData.put(emptyGap)
//                arrayData.put(mobileNo)
//                arrayData.put(emptyGap)
                arrayData.put(category)
                arrayData.put(emptyGap)
                arrayData.put(pipeSize)
                arrayData.put(emptyGap)
                arrayData.put(currentMonth)

//                arrayData.put(emptyGap)
//                arrayData.put(fwsAmount)
                if(printData?.ucn_details?.is_fws == "1"){
                    arrayData.put(emptyGap)
                    arrayData.put(waterDemand)
                    arrayData.put(emptyGap)
                    arrayData.put(rebateAmt)
                    arrayData.put(emptyGap)
                    arrayData.put(netDemand)
                    arrayData.put(emptyGap)
                    arrayData.put(serviceCharges)
                    arrayData.put(emptyGap)
                    arrayData.put(arrears)
                    arrayData.put(gaps)
                    arrayData.put(totalBalAmount)
                }else{
                    arrayData.put(emptyGap)
                    arrayData.put(currentDemand)
                    arrayData.put(emptyGap)
                    arrayData.put(arrearAmount)
                    arrayData.put(emptyGap)
                    arrayData.put(totalAmount)
                    arrayData.put(emptyGap)
                    arrayData.put(paidAmount)
                    arrayData.put(gaps)
                    arrayData.put(billAmount)
                }
                arrayData.put(gap)
                arrayData.put(sd)
                arrayData.put(ceo)
                arrayData.put(emptyGap)
                arrayData.put(saveWater)
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
                printData?.ucn_details?.let { details ->
                    findNavController().navigate(
                        BillDetailsFragmentDirections.actionBillDetailsFragmentToGenerateBillFragment(
                            request,
                            it.service_charges.toString(),
                           args.customerResponse,
                            printData?.ucn_details?.meter_no,
                            details
                        )
                    )
                }

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
                        28, "Total Amount :", printData?.bill?.total_amount.toString()
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

//                val amount = (printData!!.bill!!.net_amount.toDouble()) - (printData!!.bill!!.paid_amount.toDouble())
                val balanceAmount = JSONObject()
                balanceAmount.put("PrintDataType", 0)
                balanceAmount.put("PrinterWidth", 28)
                balanceAmount.put(
                    "DataToPrint",
                    formatText(28, "Balance Amt :", printData?.bill?.net_amount.toString())
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

                val emptyGap = JSONObject()
                emptyGap.put("PrintDataType", 0)
                emptyGap.put("PrinterWidth", 24)
                emptyGap.put("DataToPrint", "  ")
                emptyGap.put("IsCenterAligned", true)
                emptyGap.put("ImagePath", "")
                emptyGap.put("ImageData", "")

                val gaps = JSONObject()
                gaps.put("PrintDataType", 0)
                gaps.put("PrinterWidth", 24)
                gaps.put("DataToPrint", " --------------------  ")
                gaps.put("IsCenterAligned", true)
                gaps.put("ImagePath", "")
                gaps.put("ImageData", "")

                val sd = JSONObject()
                sd.put("PrintDataType", 0)
                sd.put("PrinterWidth", 28)
                sd.put(
                    "DataToPrint",
                    formatText(
                        28,
                        "",
                        "Sd/-"
                    )
                )

                sd.put("IsCenterAligned", false)
                sd.put("ImagePath", "")
                sd.put("ImageData", "")

                val ceo = JSONObject()
                ceo.put("PrintDataType", 0)
                ceo.put("PrinterWidth", 28)
                ceo.put(
                    "DataToPrint",
                    formatText(
                        28,
                        "",
                        "For CEO, SCB"
                    )
                )

                ceo.put("IsCenterAligned", false)
                ceo.put("ImagePath", "")
                ceo.put("ImageData", "")

                val saveWater = JSONObject()
                saveWater.put("PrintDataType", 0)
                saveWater.put("PrinterWidth", 28)
                saveWater.put("DataToPrint", "SAVE WATER.WATER IS PRECIOUS")
                saveWater.put("IsCenterAligned", true)
                saveWater.put("ImagePath", "")
                saveWater.put("ImageData", "")

                arrayData.put(scbName)
                arrayData.put(emptyGap)
                arrayData.put(dateTime)
                arrayData.put(emptyGap)
                arrayData.put(saleName)
                arrayData.put(emptyGap)
                arrayData.put(paidBy)
                arrayData.put(emptyGap)
                arrayData.put(baseAmount)
                arrayData.put(emptyGap)
                arrayData.put(notRequired)
                arrayData.put(imageData)
                arrayData.put(emptyGap)
                arrayData.put(receiptNo)
                arrayData.put(emptyGap)
                arrayData.put(ucnNo)
                arrayData.put(emptyGap)
                arrayData.put(ownerName)
                arrayData.put(emptyGap)
                arrayData.put(plotNo)
                arrayData.put(emptyGap)
                arrayData.put(location)
                arrayData.put(emptyGap)
                arrayData.put(mobileNo)
                arrayData.put(emptyGap)
                arrayData.put(totalAmount)
                arrayData.put(gaps)
                arrayData.put(balanceAmount)
                arrayData.put(gap)
                arrayData.put(sd)
                arrayData.put(ceo)
                arrayData.put(emptyGap)
                arrayData.put(saveWater)
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
                binding.preReading.text = printData?.bill?.present_reading ?: "0"
                binding.prevReading.text = printData?.bill?.previous_reading ?: "0"
                binding.units.text = printData?.bill?.units ?: "0"
                binding.meter.text = printData?.ucn_details?.meter_no ?: ""
                binding.categoryText.text = printData?.ucn_details?.category?:""
                if (args.customerResponse.is_fws == 1) {
                    if (printData?.ucn_details?.is_bill_generated == "0") {
                        binding.billText.isVisible = false
                        binding.viewBill.isVisible = true
                    } else {
                        binding.billText.isVisible = true
                        binding.viewBill.isVisible = false
                    }
                }
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