package com.scb.scbbillingandcollection.auth.presentation.viewmodels

import androidx.lifecycle.viewModelScope
import com.scb.scbbillingandcollection.auth.data.models.LoginRequest
import com.scb.scbbillingandcollection.auth.data.models.LoginResponse
import com.scb.scbbillingandcollection.auth.data.repository.AuthRepository
import com.scb.scbbillingandcollection.core.base.ActionViewModel
import com.scb.scbbillingandcollection.core.base.BaseAction
import com.scb.scbbillingandcollection.core.retrofit.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(private val repo: AuthRepository) :
    ActionViewModel<AuthActions>() {
    private val _versionResponse = MutableSharedFlow<AuthState.VersionData>()
    var versionResponse = _versionResponse.asSharedFlow()

    private val _loginResponse = MutableStateFlow(AuthState.LoginData())
    var loginResponse = _loginResponse.asStateFlow()

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
                    _loginResponse.update {
                        it.copy(response.value, null)
                    }
                } else {
                    _loginResponse.update {
                        it.copy(null, response.value.message)
                    }
                }
            }

            is Resource.Failure -> {
                _loginResponse.update {
                    it.copy(null, response.errorBody.toString())
                }
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
}

