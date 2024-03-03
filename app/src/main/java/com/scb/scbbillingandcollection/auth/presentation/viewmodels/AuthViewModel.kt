package com.scb.scbbillingandcollection.auth.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.scb.scbbillingandcollection.auth.data.models.LoginRequest
import com.scb.scbbillingandcollection.auth.data.models.LoginResponse
import com.scb.scbbillingandcollection.auth.data.repository.AuthRepository
import com.scb.scbbillingandcollection.core.base.ActionViewModel
import com.scb.scbbillingandcollection.core.base.BaseAction
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.generate_bill.data.models.WardsResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(private val repo: AuthRepository) :
    ActionViewModel<AuthActions>() {
    private val _versionResponse = MutableSharedFlow<AuthState.VersionData>()
    var versionResponse = _versionResponse.asSharedFlow()

    private val _loginResponse = MutableSharedFlow<AuthState.LoginData>()
    var loginResponse = _loginResponse.asSharedFlow()

    private val _wardsResponse = MutableStateFlow(AuthState.WardsData())
    var wardsResponse = _wardsResponse.asStateFlow()

    override suspend fun bindActions() {
        actions.collectLatest { action ->
            when (action) {
                is AuthActions.LoginAction -> {
                    checkLogin(action.request)
                }
            }
        }
    }

    private suspend fun checkLogin(request: LoginRequest) {
        when (val response = repo.loginCheck(request)) {
            is Resource.Success -> {
                if (response.value.error == 0) {
                    _loginResponse.emit(AuthState.LoginData(response.value,null))
                } else {
                    _loginResponse.emit(AuthState.LoginData(null,response.value.message))
                }
            }

            is Resource.Failure -> {
                _loginResponse.emit(AuthState.LoginData(null,response.errorBody.toString()))
            }

            else -> {}
        }

    }

    init {
        viewModelScope.launch {
            getVersionCheck()
        }
    }

    private suspend fun getVersionCheck() {
        when (val response = repo.getVersionCheck()) {
            is Resource.Success -> {
                if (response.value.response?.appUpdate == 2) {
                    _versionResponse.emit(AuthState.VersionData(true))
                }
            }

            is Resource.Failure -> {

            }

            else -> {}
        }

    }



}

sealed class AuthActions : BaseAction {
    data class LoginAction(var request: LoginRequest) : AuthActions()
}

sealed class AuthState {
    data class VersionData(val canUpgradable: Boolean = false)
    data class LoginData(val loginData: LoginResponse? = null, val error: String? = null)
    data class WardsData(val loginData: WardsResponse? = null, val error: String? = null)
}

