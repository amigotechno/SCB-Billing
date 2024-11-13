package com.scb.scbbillingandcollection.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.scb.scbbillingandcollection.collect_bill.models.CollectionDetails
import com.scb.scbbillingandcollection.collect_bill.models.Receipts
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.diffChecker
import com.scb.scbbillingandcollection.databinding.CollectionDetailsItemBinding
import com.scb.scbbillingandcollection.databinding.CollectionItemBinding

class ReportDetailsAdapter(private val onClick: (CollectionDetails.Receipt) -> Unit) :
    ListAdapter<CollectionDetails.Receipt, ReportDetailsAdapter.ViewHolder>(diffChecker { oldItem, newItem -> oldItem.toString() == newItem.toString() }) {

    class ViewHolder(val itemAttachmentBinding: CollectionDetailsItemBinding) :
        RecyclerView.ViewHolder(itemAttachmentBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = CollectionDetailsItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = currentList[position]
        holder.itemAttachmentBinding.apply {
            ucnValue.text = currentItem.ucn_number.toString()
            nameValue.text = currentItem.consumer_name
            plotValue.text = currentItem.plot_no
            locValue.text = currentItem.location
            billValue.text = currentItem.bill_no
            paidValue.text = currentItem.amount.toString()
            chequeValue.text = currentItem.cheque_dd_no?:"NA"
            chequeDateValue.text = currentItem.cheque_dd_date?:"NA"
            bankValue.text = currentItem.cheque_dd_bank?:"NA"
        }
        holder.itemView.rootView.clickWithDebounce {
            onClick(currentItem)
        }
    }
}
