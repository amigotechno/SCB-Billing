package com.scb.scbbillingandcollection.collect_bill

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
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blankj.utilcode.util.ToastUtils
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.collect_bill.models.GetCanId
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.millisToDate
import com.scb.scbbillingandcollection.core.extensions.millisToTime
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.core.utils.Constants
import com.scb.scbbillingandcollection.databinding.FragmentCollectBillDetailsBinding
import com.scb.scbbillingandcollection.generate_bill.data.models.DemandAndCollectBill
import com.scb.scbbillingandcollection.generate_bill.data.repository.GenerateBillRepositoryImpl
import com.scb.scbbillingandcollection.generate_bill.presentation.ui.BillDetailsFragmentDirections
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
class CollectBillDetailsFragment : Fragment() {

    private var _binding : FragmentCollectBillDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: CollectBillDetailsFragmentArgs by navArgs()

    private var printData: DemandAndCollectBill? = null

    lateinit var hexString: StringBuilder

    @Inject
    lateinit var repImpl: GenerateBillRepositoryImpl

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCollectBillDetailsBinding.inflate(layoutInflater,container,false)

        val imageBytes = getDrawableAsBytes(requireContext(), R.drawable.logo)
        hexString = StringBuilder()
        imageBytes.forEach {
            hexString.append(String.format("%02X", it))
        }
        binding.nameTxt.text = args.customerResponse.consumer_name
        binding.addressText.text = args.customerResponse.location
        binding.categoryText.text = args.customerResponse.category
        binding.pipeSizeTxt.text = args.customerResponse.pipe_size
        binding.meterNoText.text = args.customerResponse.can_number
        binding.lastBilledText.text = args.customerResponse.last_billed_date

        binding.collectBill.clickWithDebounce {
            findNavController().navigate(CollectBillDetailsFragmentDirections.actionCollectBillDetailsFragmentToCollectTypeFragment(args.customerResponse))
        }

        binding.updateSCB.clickWithDebounce {
            findNavController().navigate(CollectBillDetailsFragmentDirections.actionCollectBillDetailsFragmentToUpdateSCBFragment(args.customerResponse.id.toString()))
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

        lifecycleScope.launch {
            getPrintData()
        }


        return binding.root
    }
    private fun setData() {
        val intent = Intent()
        intent.setAction("com.pinelabs.masterapp.SERVER")
        intent.setPackage("com.pinelabs.masterapp")
        requireActivity().bindService(intent, printConnection, AppCompatActivity.BIND_AUTO_CREATE)
    }

    private fun getReceipt() {
        val intent = Intent()
        intent.setAction("com.pinelabs.masterapp.SERVER")
        intent.setPackage("com.pinelabs.masterapp")
        requireActivity().bindService(intent, receiptConnection, AppCompatActivity.BIND_AUTO_CREATE)
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
                arrayData.put(emptyGap)
                arrayData.put(currentDemand)
                arrayData.put(emptyGap)
                arrayData.put(arrearAmount)
                arrayData.put(emptyGap)
                arrayData.put(fwsAmount)
                arrayData.put(emptyGap)
                arrayData.put(totalAmount)
                arrayData.put(emptyGap)
                arrayData.put(paidAmount)
                arrayData.put(gaps)
                arrayData.put(billAmount)
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
                        "" ,
                        "Sd/-")
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
                        "" ,
                        "For CEO, SCB")
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


    private suspend fun getPrintData() {
        val response = repImpl.printData(GetCanId(args.customerResponse.id.toString()))
        when (response) {
            is Resource.Success -> {
                printData = response.value
                binding.serviceChargesText.text = printData?.bill?.service_charges
                binding.payableText.text = printData?.bill?.payable_amount
                binding.categoryText.text = printData?.ucn_details?.plot_no
                binding.demandText.text = printData?.bill?.current_month_demand
                binding.arrearsText.text = printData?.bill?.arrear
                binding.klLabelText.text = printData?.bill?.rebate_amount
                binding.netText.text = printData?.bill?.net_amount
            }

            is Resource.Failure -> {
                ToastUtils.showShort(response.errorBody.toString())
            }

            else -> {}
        }
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