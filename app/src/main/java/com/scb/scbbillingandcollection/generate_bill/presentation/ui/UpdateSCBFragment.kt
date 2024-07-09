package com.scb.scbbillingandcollection.generate_bill.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blankj.utilcode.util.ToastUtils
import com.scb.scbbillingandcollection.collect_bill.models.UpdateSCB
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.retrofit.Resource
import com.scb.scbbillingandcollection.databinding.FragmentUpdateSCBBinding
import com.scb.scbbillingandcollection.generate_bill.data.repository.GenerateBillRepositoryImpl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UpdateSCBFragment : Fragment() {

    private lateinit var binding: FragmentUpdateSCBBinding

    @Inject
    lateinit var repImpl: GenerateBillRepositoryImpl

    private val args: UpdateSCBFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUpdateSCBBinding.inflate(layoutInflater, container, false)

        binding.updateNo.clickWithDebounce {
            lifecycleScope.launch {
                if (binding.scbNo.text?.toString()?.isNotEmpty() == true) {
                    updateSCBNo()
                } else {
                    ToastUtils.showShort("Enter SCB No")
                }

            }
        }

        return binding.root
    }


    private suspend fun updateSCBNo() {
        val response = repImpl.updateSCBNo(UpdateSCB(args.canId, binding.scbNo.text.toString()))
        when (response) {
            is Resource.Success -> {
                if (response.value?.error == 0) {
                    findNavController().popBackStack()
                }
                ToastUtils.showShort(response.value.message)
            }

            is Resource.Failure -> {
                ToastUtils.showShort(response.errorBody.toString())
            }

            else -> {}
        }
    }

}