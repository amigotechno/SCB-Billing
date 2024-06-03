package com.scb.scbbillingandcollection.generate_bill.presentation.ui

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.birjuvachhani.locus.Locus
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.ToastUtils
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.configureLocus
import com.scb.scbbillingandcollection.core.extensions.millisToDate
import com.scb.scbbillingandcollection.core.extensions.millisToTime
import com.scb.scbbillingandcollection.core.extensions.observerSharedFlow
import com.scb.scbbillingandcollection.core.extensions.showCustomToast
import com.scb.scbbillingandcollection.core.extensions.showDialog
import com.scb.scbbillingandcollection.core.utils.Constants
import com.scb.scbbillingandcollection.databinding.FragmentGenerateBillBinding
import com.scb.scbbillingandcollection.generate_bill.data.models.GenerateBillRequest
import com.scb.scbbillingandcollection.generate_bill.presentation.viewmodel.GenerateBillViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

@AndroidEntryPoint
class GenerateBillFragment : Fragment() {
    var currentPhotoPath: String = ""
    private var _binding: FragmentGenerateBillBinding? = null
    private val ALL_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION
    )

    lateinit var hexString: StringBuilder

    //    lateinit var amigoHexString: StringBuilder
    private val binding get() = _binding!!
    private val billViewModel: GenerateBillViewModel by navGraphViewModels(R.id.main_nav_graph) { defaultViewModelProviderFactory }

    private var photo_Encoded = ""
    private val args: GenerateBillFragmentArgs by navArgs()

    var receiptNo = ""
    var billDate = ""
    val CAMERA_REQUEST_CODE = 102
    var latitude = "0.0"
    var longitude = "0.0"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentGenerateBillBinding.inflate(layoutInflater, container, false)
        initObservers()

        val imageBytes = getDrawableAsBytes(requireContext(), R.drawable.logo)
        hexString = StringBuilder()
        imageBytes.forEach {
            hexString.append(String.format("%02X", it))
        }

//        amigoHexString = StringBuilder()
//        amigoImageBytes.forEach {
//            amigoHexString.append(String.format("%02X", it))
//        }

        binding.generateBill.clickWithDebounce {
            binding.generateBill.isEnabled = false
            if (photo_Encoded != "") {
                val request = GenerateBillRequest(
                    args.request.can_id,
                    args.request.reading,
                    args.request.meter_status,
                    args.request.current_month_demand,
                    args.request.rebate_amt,
                    args.request.arrear,
                    args.request.net_amount,
                    photo_Encoded,
                    latitude, longitude

                )
                billViewModel.dispatch(GenerateBillViewModel.BillActions.GenerateBill(request))

            } else {
                billViewModel.dispatch(GenerateBillViewModel.BillActions.GenerateBill(args.request))
            }
        }

        binding.cancel.clickWithDebounce {
            findNavController().popBackStack()
        }
        binding.apply {
            demandText.text = args.request.current_month_demand.toString()
            serviceChargesText.text = args.charges.toString()
            arrearsText.text = args.request.arrear.toString()
//            klLabelText.text = args.request.rebate_amt.toString()
            netText.text = args.request.net_amount.toString()
            generateBill.isEnabled = true
        }

        binding.meterImage.setOnClickListener {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionUtils.isGranted(*ALL_PERMISSIONS)) {
                PermissionUtils.permission(*ALL_PERMISSIONS)
                    .callback(object : PermissionUtils.SimpleCallback {
                        override fun onGranted() {
                            dispatchTakePictureIntent()
                        }

                        override fun onDenied() {
                            ToastUtils.showShort("Accept all permissions to access app")
                        }
                    }).request()
            } else {
                dispatchTakePictureIntent()
            }
//            } else {
//                if (!PermissionUtils.isGranted(*ANDROID10_ALL_PERMISSIONS)) {
//                    PermissionUtils.permission(*ANDROID10_ALL_PERMISSIONS)
//                        .callback(object : PermissionUtils.SimpleCallback {
//                            override fun onGranted() {
//                                dispatchTakePictureIntent()
//                            }
//
//                            override fun onDenied() {
//                                ToastUtils.showShort("Accept all permissions to access app")
//                            }
//                        }).request()
//                } else {
//                    dispatchTakePictureIntent()
//                }
//            }
        }
//        getLocation()

        return binding.root
    }

    fun getLocation() {
        Locus.configureLocus(
            requireBackgroundUpdate = true, forceRequireBackgroundUpdate = true
        ).getCurrentLocation(requireContext()) { result ->
            result.location?.let { location ->
                latitude = location.latitude.toString()
                longitude = location.longitude.toString()

                Log.d("Latitude", "getLocationForPunch: " + location.latitude)
            }
            result.error?.let {
//                getLocation()
            }
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


    private fun setData() {
        val intent = Intent()
        intent.setAction("com.pinelabs.masterapp.SERVER")
        intent.setPackage("com.pinelabs.masterapp")
        requireActivity().bindService(intent, printConnection, AppCompatActivity.BIND_AUTO_CREATE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    val f = File(currentPhotoPath)
                    binding.meterImage.setImageURI(Uri.fromFile(f))
                    var bitmap: Bitmap
                    val options = BitmapFactory.Options()
                    options.inSampleSize = 8
                    bitmap = BitmapFactory.decodeFile(
                        currentPhotoPath, options
                    )
                    try {
                        //Encode to Stringche
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, byteArrayOutputStream)
                        val byteArray = byteArrayOutputStream.toByteArray()
                        photo_Encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                    } catch (e: Exception) {
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

    }


    @Throws(IOException::class)
    fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "SCB${timeStamp}_", /* prefix */
            ".jpeg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath

        }
    }

    fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(requireContext().packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                val photoURI: Uri = FileProvider.getUriForFile(
                    requireActivity(), "com.scb.scbbillingandcollection.fileprovider", photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
            }
//            getLocation()
        }

    }

    private fun initObservers() {


        observerSharedFlow(billViewModel.generateBillResponse) {response->
            response.billNo?.let {
                showCustomToast(R.drawable.ic_check_green, title = it)
                receiptNo = it
                billDate = response.billDate.toString()
                findNavController().navigate(
                    GenerateBillFragmentDirections.actionGenerateFragmentToGenerateCanListFragment(
                        true
                    )
                )
                setData()
            }
            response.error?.let {
                binding.generateBill.isEnabled = true
                showCustomToast(R.drawable.ic_error_warning, title = it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                        "DataToPrint", "Name : " + args.customerResponse.consumer_name.toString()
                    )
                } else {
                    ownerName.put(
                        "DataToPrint",
                        formatText(28, "Name :", args.customerResponse.consumer_name.toString())
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
                        "DataToPrint", "Address : " + args.customerResponse.location.toString()
                    )
                } else {
                    location.put(
                        "DataToPrint",
                        formatText(28, "Address :", args.customerResponse.location.toString())
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
                    formatText(28, "Current Demand :", args.request.current_month_demand.toString())
                )
                currentDemand.put("IsCenterAligned", false)
                currentDemand.put("ImagePath", "")
                currentDemand.put("ImageData", "")

                val currentMonth = JSONObject()
                currentMonth.put("PrintDataType", 0)
                currentMonth.put("PrinterWidth", 28)
                currentMonth.put("DataToPrint", formatText(28, "Curr Mth :", billDate))
                currentMonth.put("IsCenterAligned", false)
                currentMonth.put("ImagePath", "")
                currentMonth.put("ImageData", "")

                val billNo = JSONObject()
                billNo.put("PrintDataType", 0)
                billNo.put("PrinterWidth", 28)
                billNo.put("DataToPrint", formatText(28, "Bill No :", receiptNo))
                billNo.put("IsCenterAligned", false)
                billNo.put("ImagePath", "")
                billNo.put("ImageData", "")

                val arrearAmount = JSONObject()
                arrearAmount.put("PrintDataType", 0)
                arrearAmount.put("PrinterWidth", 28)
                arrearAmount.put(
                    "DataToPrint",
                    formatText(28, "Arrears :", args.request.arrear.toString())
                )
                arrearAmount.put("IsCenterAligned", false)
                arrearAmount.put("ImagePath", "")
                arrearAmount.put("ImageData", "")

                val billAmount = JSONObject()
                billAmount.put("PrintDataType", 0)
                billAmount.put("PrinterWidth", 28)
                billAmount.put(
                    "DataToPrint",
                    formatText(28, "Bill Amt :", args.request.net_amount.toString())
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
                arrayData.put(noticeTitle)
                arrayData.put(emptyGap)
                arrayData.put(dateTime)
                arrayData.put(imageData)
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
                arrayData.put(currentMonth)
                arrayData.put(emptyGap)
                arrayData.put(billNo)
                arrayData.put(emptyGap)
                arrayData.put(currentDemand)
                arrayData.put(emptyGap)
                arrayData.put(arrearAmount)
                arrayData.put(emptyGap)
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


    fun formatText(receiptWidth: Int, leftText: String, rightText: String): String {
        val leftWidth = receiptWidth / 2
        val rightWidth = receiptWidth - leftWidth
        val formattedLeftText = leftText.take(leftWidth)
        val formattedRightText = rightText.takeLast(rightWidth)
        return "$formattedLeftText${" ".repeat(receiptWidth - formattedLeftText.length - formattedRightText.length)}$formattedRightText"
    }

    fun showDialog() {
        requireContext().showDialog(title = "LogOut",
            description = "Are You Sure to Logout?",
            "LogOut",
            "Cancel",
            negativeButtonFunction = {
                findNavController().navigate(
                    GenerateBillFragmentDirections.actionGenerateFragmentToGenerateCanListFragment(
                        true
                    )
                )
            },
            positiveButtonFunction = {
                findNavController().navigate(
                    GenerateBillFragmentDirections.actionGenerateFragmentToCollectBillDetails(args.customerResponse)
                )
            })
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
                val obj = JSONObject(response)

                if (obj.getString("ResponseMsg").equals("APPROVED")) {

                    showDialog()
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            Log.e("Tagresponse", value)
        }
    }
}