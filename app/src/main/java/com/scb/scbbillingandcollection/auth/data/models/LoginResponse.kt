package com.scb.scbbillingandcollection.auth.data.models

data class LoginResponse(
    val error: Int?,
    val message: String?,
    val token: String?,
    val user_data: UserData?
) {
    data class UserData(
        val created: String?,
        val email: String?,
        val id: Int?,
        val modified: String?,
        val name: String?,
        val password_reset: String?,
        val phone: String?,
        val role_id: Int?,
        val status: Int?,
        val username: String?
    )
}

data class LoginRequest(
    val username: String,
    val password: String
)