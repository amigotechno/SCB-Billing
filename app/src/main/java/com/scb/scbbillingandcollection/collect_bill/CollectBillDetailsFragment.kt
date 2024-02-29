package com.scb.scbbillingandcollection.collect_bill

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.scb.scbbillingandcollection.R
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.databinding.FragmentCollectBillDetailsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollectBillDetailsFragment : Fragment() {

    private var _binding : FragmentCollectBillDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: CollectBillDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCollectBillDetailsBinding.inflate(layoutInflater,container,false)
        binding.addressText.text = args.customerResponse.location
        binding.arrearsText.text = args.customerResponse.arrears
        binding.categoryText.text = args.customerResponse.category
        binding.demandText.text = args.customerResponse.demand
        binding.pipeSizeTxt.text = args.customerResponse.pipe_size
        binding.klLabelText.text = args.customerResponse.net_demand
        binding.netText.text = args.customerResponse.payable_amount
        binding.meterNoText.text = args.customerResponse.can_number

        binding.collectBill.clickWithDebounce {
            findNavController().navigate(CollectBillDetailsFragmentDirections.actionCollectBillDetailsFragmentToCollectTypeFragment(args.customerResponse))
        }

        return binding.root
    }


}