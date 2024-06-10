package com.scb.scbbillingandcollection.collect_bill.models

data class CollectionModel(
    val error: Int,
    val receipts: MutableList<Receipts>
)

data class Receipts(
    val collect_type: String,
    val total: String
)