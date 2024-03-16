package com.scb.scbbillingandcollection.auth.presentation.ui

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
import androidx.appcompat.app.AppCompatActivity
import com.scb.scbbillingandcollection.R
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        setData()
    }

    private fun setData() {
        val intent = Intent()
        intent.setAction("com.pinelabs.masterapp.SERVER")
        intent.setPackage("com.pinelabs.masterapp")
        bindService(intent, printConnection, AppCompatActivity.BIND_AUTO_CREATE)
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
                val billEntries =
                    arrayOf(arrayOf("Water Usage", "$50.00"), arrayOf("Service Charge", "$20.00"))

                val billText = StringBuilder()


                // Entries with left and right alignment
                for (entry in billEntries) {
                    billText.append(String.format("%-10s%10s\n", entry[0], entry[1]))
                }


                printData.put("DataToPrint",billText)
                printData.put("IsCenterAligned", true)
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
    private fun calculateTotal(billEntries: Array<Array<String>>): Double {
        var total = 0.0
        for (entry in billEntries) {
            total += entry[1].replace("$", "").toDouble()
        }
        return total
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