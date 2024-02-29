package com.scb.scbbillingandcollection.collect_bill

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.databinding.FragmentCollectBillDetailsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollectBillDetailsFragment : Fragment() {

    private var _binding : FragmentCollectBillDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCollectBillDetailsBinding.inflate(layoutInflater,container,false)
        binding.collectBill.clickWithDebounce {
            findNavController().navigate(CollectBillDetailsFragmentDirections.actionCollectBillDetailsFragmentToCollectTypeFragment())
        }
        return binding.root
    }


}