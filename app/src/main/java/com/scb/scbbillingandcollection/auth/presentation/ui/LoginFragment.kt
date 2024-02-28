package com.scb.scbbillingandcollection.auth.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.auth.data.models.LoginRequest
import com.scb.scbbillingandcollection.auth.presentation.viewmodels.AuthActions
import com.scb.scbbillingandcollection.auth.presentation.viewmodels.AuthViewModel
import com.scb.scbbillingandcollection.core.base.AppPreferences
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.hideKeyboard
import com.scb.scbbillingandcollection.core.extensions.observerSharedFlow
import com.scb.scbbillingandcollection.core.extensions.observerState
import com.scb.scbbillingandcollection.core.extensions.showCustomToast
import com.scb.scbbillingandcollection.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        initObservers()
        binding.login.clickWithDebounce {
            hideKeyboard()
            if (binding.username.text.isEmpty()) {
                showCustomToast(R.drawable.ic_error_warning, title = "Enter Username")
            } else if (binding.password.text.isEmpty()) {
                showCustomToast(R.drawable.ic_error_warning, title = "Enter Password")
            } else {
                val request = LoginRequest(
                    username = binding.username.text.toString(),
                    password = binding.password.text.toString()
                )
                viewModel.dispatch(AuthActions.LoginAction(request))
            }

        }
        return binding.root
    }

    private fun initObservers() {

        observerSharedFlow(viewModel.versionResponse) {
            if (it.canUpgradable) {

            }
        }

        observerState(viewModel.loginResponse) {
            it.loginData?.let { response ->
                response.apply {
                    appPreferences.token = token ?: ""
                    appPreferences.isLoggedIn = true
                    appPreferences.name = user_data?.get(0)?.name ?: ""
                    appPreferences.userId = user_data?.get(0)?.id.toString()
                    appPreferences.roleId = user_data?.get(0)?.role_id.toString()
                }
                showCustomToast(R.drawable.ic_check_green, title = response.message ?: "Success")
                findNavController().navigate(R.id.dashboardFragment)
            }
            it.error?.let {
                showCustomToast(R.drawable.ic_error_warning, title = it)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}