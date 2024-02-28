package com.scb.scbbillingandcollection.collect_bill

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.scb.scbbillingandcollection.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollectBillDetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_collect_bill_details, container, false)
    }


}