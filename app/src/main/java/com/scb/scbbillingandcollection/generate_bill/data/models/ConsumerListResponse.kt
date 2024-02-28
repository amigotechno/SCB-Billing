package com.scb.scbbillingandcollection.generate_bill.data.models

data class ConsumerListResponse(val consumers_list: List<Consumers?>?)

data class Consumers(
    val can_number: String?,
    val category: String?,
    val consumer_name: String?,
    val id: Int?,
    val location: String?,
    val phone_no: String?,
    val pipe_size: String?,
    val scb_no: String?,
    val sub_category: String?
)
