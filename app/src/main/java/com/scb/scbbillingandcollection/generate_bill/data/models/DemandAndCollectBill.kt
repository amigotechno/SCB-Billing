package com.scb.scbbillingandcollection.generate_bill.data.models

data class DemandAndCollectBill(
    val bill: Bill,
    val receipt: Receipt,
    val ucn_details: UcnDetailsX
)

data class Bill(
    val arrear: String,
    val bill_no: String,
    val billdate: String,
    val current_month_demand: String,
    val id: String,
    val net_amount: String,
    val paid_amount: String,
    val payable_amount: String,
    val rebate_amount: String,
    val service_charges: String
)

data class Receipt(
    val amount: String,
    val cheque_dd_bank: String,
    val cheque_dd_branch: String,
    val cheque_dd_no: String,
    val cheque_dddate: String,
    val collect_type: String,
    val id: String,
    val receipt_date: String,
    val receipt_no: String
)

data class UcnDetailsX(
    val can_number: String,
    val consumer_name: String,
    val id: String,
    val location: String,
    val plot_no: String
)