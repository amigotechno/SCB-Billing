package com.scb.scbbillingandcollection.collect_bill.models

data class CollectionDetails(
    val error: Int?,
    val receipts: List<Receipt?>?
) {
    data class Receipt(
        val amount: Int?,
        val bill_no: String?,
        val cheque_dd_bank: String?,
        val cheque_dd_branch: String?,
        val cheque_dd_date: String?,
        val cheque_dd_no: String?,
        val consumer_name: String?,
        val location: String?,
        val plot_no: String?,
        val ucn_number: String?
    )
}