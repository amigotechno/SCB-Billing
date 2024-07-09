package com.scb.scbbillingandcollection.generate_bill.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class DemandAndCollectBill(
    val bill: Bill?,
    val receipt: Receipt?,
    val ucn_details: UcnDetailsX?
)

data class Bill(
    val arrear: String?,
    val bill_no: String?,
    val billdate: String?,
    val current_month_demand: String?,
    val id: String?,
    val net_amount: String?,
    val paid_amount: String?,
    val payable_amount: String?,
    val present_reading: String?,
    val presentreading_date: String?,
    val previous_reading: String?,
    val previousreading_date: String?,
    val rebate_amount: String?,
    val service_charges: String?,
    val total_amount: String?,
    val units: String?
)

data class Receipt(
    val amount: String?,
    val cheque_dd_bank: String?,
    val cheque_dd_branch: String?,
    val cheque_dd_no: String?,
    val cheque_dddate: String?,
    val collect_type: String?,
    val id: String?,
    val receipt_date: String?,
    val receipt_no: String?
)

@Parcelize
data class UcnDetailsX(
    val aadhar_status: String?,
    val can_number: String?,
    val category: String?,
    val consumer_name: String?,
    val id: String?,
    val is_bill_generated: String?,
    val is_fws: String?,
    val last_billed_date: String?,
    val location: String?,
    val meter_no: String?,
    val meter_status: String?,
    val net_demand: String?,
    val no_of_aadhar_reg: String?,
    val pipe_size: String?,
    val plot_no: String?
):Parcelable