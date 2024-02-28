package com.scb.scbbillingandcollection.auth.data.models

data class VersionResponse(
    val response: Response?
) {
    data class Response(
        val appUpdate: Int?,
        val error: Int?
    )
}