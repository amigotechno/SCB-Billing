package com.scb.scbbillingandcollection.collect_bill.models

data class CollectBillRequest(

    val can_id: String,
    val collect_type: String,
    val amount: String? = null,
    val dd_no: String? = null,
    val dd_bank: String? = null,
    val dd_branch: String? = null,
    val dd_date: String? = null,
    val cheque_no: String? = null,
    val cheque_bank: String? = null,
    val cheque_branch: String? = null,
    val cheque_date: String? = null
)


data class CollectBillResponse(
    val error: String, val receipt_no: String?, val message: String?
)

data class CansRequest(
    val ward: String, val beat_code: String? = ""
)

data class GetCan(
    val can_number: String? = ""
)
data class GetCanId(
    val can_id: String? = ""
)