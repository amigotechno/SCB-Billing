package com.scb.scbbillingandcollection.generate_bill.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class ViewBillResponse(
    val amounts: Amounts?,
    val error: Int?,
    val message: String?,
) {
    data class Amounts(
        val arrear: Int?,
        val current_month_demand: Int?,
        val net_amount: Int?,
        val rebate_amt: Int?,
        val service_charges: Int?,
        val water_cess: Int?
    )
}


data class ViewBillRequest(
    val can_id: String,
    val reading: String,
    val meter_status: String
)

@Parcelize
data class GenerateBillRequest(
    val can_id: String,
    val reading: String,
    val meter_status: String,
    val current_month_demand: String,
    val rebate_amt: String,
    val arrear: String,
    val net_amount: String
):Parcelable

data class GenerateBillResponse(
    val error:String?,
    val bill_number:String?,
    val message:String?,
)

