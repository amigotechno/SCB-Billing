package com.scb.scbbillingandcollection.auth.presentation.ui

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.core.base.AppPreferences
import com.scb.scbbillingandcollection.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSplashBinding.inflate(layoutInflater, container, false)

        Handler().postDelayed({
            if (appPreferences.isLoggedIn) {
                findNavController().navigate(R.id.dashboardFragment)
            } else {
                findNavController().navigate(R.id.loginFragment)
            }
        }, 2000)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}