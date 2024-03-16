package com.scb.scbbillingandcollection.generate_bill.presentation.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
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
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.millisToDate
import com.scb.scbbillingandcollection.core.extensions.observerSharedFlow
import com.scb.scbbillingandcollection.core.extensions.showCustomToast
import com.scb.scbbillingandcollection.databinding.FragmentGenerateBillBinding
import com.scb.scbbillingandcollection.generate_bill.presentation.viewmodel.GenerateBillViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@AndroidEntryPoint
class GenerateBillFragment : Fragment() {

    private var _binding: FragmentGenerateBillBinding? = null
    private val binding get() = _binding!!
    private val billViewModel: GenerateBillViewModel by navGraphViewModels(R.id.main_nav_graph) { defaultViewModelProviderFactory }

    private val args: GenerateBillFragmentArgs by navArgs()

    var receiptNo = "123423"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentGenerateBillBinding.inflate(layoutInflater, container, false)
        initObservers()

        binding.generateBill.clickWithDebounce {
            billViewModel.dispatch(GenerateBillViewModel.BillActions.GenerateBill(args.request))
        }
        binding.apply {
            demandText.text = args.request.current_month_demand.toString()
            serviceChargesText.text = args.charges.toString()
            arrearsText.text = args.request.arrear.toString()
//            klLabelText.text = args.request.rebate_amt.toString()
            netText.text = args.request.net_amount.toString()
            generateBill.isEnabled = true
        }

        return binding.root
    }

    private fun setData() {
        val intent = Intent()
        intent.setAction("com.pinelabs.masterapp.SERVER")
        intent.setPackage("com.pinelabs.masterapp")
        requireActivity().bindService(intent, printConnection, AppCompatActivity.BIND_AUTO_CREATE)
    }

    private fun initObservers() {


        observerSharedFlow(billViewModel.generateBillResponse) {
            it.data?.let {
                showCustomToast(R.drawable.ic_check_green, title = it)
                receiptNo = it
                setData()
                findNavController().navigate(
                    GenerateBillFragmentDirections.actionGenerateFragmentToGenerateCanListFragment(
                        true
                    )
                )

            }
            it.error?.let {
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
//                printData.put("DataToPrint", "----------------------------"+"\n"+"\n"+"\n"+"Secunderabad Containment Board"+"\n"+"Water Bill"+"\n"+"Bill No : "+receiptNo+"\n"+"USN NO: "+args.request.can_id+"\n"+"Demand Amount :"+args.request.current_month_demand+"\n"+
//                "Arrears Amount : "+args.request.arrear+"\n"+"Service charges : "+args.charges+"\n"+"Total Amount : "+args.request.net_amount+"\n"+"\n"+"\n"+"\n"+"\n"+"\n"+"--------------------"+"\n"+"\n")

                val waterBillString = getString(R.string.water_bill)
                val formattedWaterBill = waterBillString
                    .replace("[Bill]", millisToDate(System.currentTimeMillis()))
                    .replace("[Due]", millisToDate(System.currentTimeMillis()+518400000))
                    .replace("[Receipt]", receiptNo)
                    .replace("[USN]", args.request.can_id)
                    .replace("[Name]", "Sridhar")
                    .replace("[Address]", "Secunderabad, Hyderabad")
                    .replace("[Previous]", "12234")
                    .replace("[Present]", "232434")
                    .replace("[Units]", "123")
                    .replace("[Demand]", args.request.current_month_demand)
                    .replace("[Arrears]", args.request.arrear)
                    .replace("[Service]", args.charges)
                    .replace("[Total]", args.request.net_amount)


                printData.put("DataToPrint",formattedWaterBill)
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