package com.scb.scbbillingandcollection.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.scb.scbbillingandcollection.core.base.AppPreferences
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.showDialog
import com.scb.scbbillingandcollection.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    val binding get() = _binding!!

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentDashboardBinding.inflate(layoutInflater, container, false)

        binding.name.text = appPreferences.name
        binding.generateLayout.clickWithDebounce {
            findNavController().navigate(
                DashboardFragmentDirections.actionDashboardFragmentToGenerateCanListFragment(
                    true
                )
            )
        }

        binding.collectLayout.clickWithDebounce {
            findNavController().navigate(
                DashboardFragmentDirections.actionDashboardFragmentToGenerateCanListFragment(
                    false
                )
            )
        }

        binding.logoutBtn.clickWithDebounce {
            requireContext().showDialog(title = "LogOut",
                description = "Are You Sure to Logout?",
                "LogOut",
                "Cancel",
                positiveButtonFunction = {
                    appPreferences.clearPreferencesData()
                    findNavController().navigate(
                        DashboardFragmentDirections.actionDashboardFragmentToLoginFragment()
                    )
                })

        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}