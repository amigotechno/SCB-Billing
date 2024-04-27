package com.scb.scbbillingandcollection.generate_bill.data.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

data class ConsumerListResponse(val consumers_list: List<Consumers?>?)


@Keep
@Parcelize
data class Consumers(
    val can_number: String?,
    val category: String?,
    val consumer_name: String?,
    val id: Int?,
    val location: String?,
    val phone_no: String?,
    val pipe_size: String?,
    val scb_no: String?,
    val sub_category: String?,
    val demand: String?,
    val arrears: String?,
    val net_demand: String?,
    val payable_amount: String?,
    val last_billed_date: String?,
    val plot_no: String?,
):Parcelable

