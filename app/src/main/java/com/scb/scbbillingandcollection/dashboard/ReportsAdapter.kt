package com.scb.scbbillingandcollection.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.scb.scbbillingandcollection.collect_bill.models.Receipts
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.diffChecker
import com.scb.scbbillingandcollection.databinding.CollectionItemBinding

class ReportsAdapter(private val onClick: (Receipts) -> Unit) :
    ListAdapter<Receipts, ReportsAdapter.ViewHolder>(diffChecker { oldItem, newItem -> oldItem.toString() == newItem.toString() }) {

    class ViewHolder(val itemAttachmentBinding: CollectionItemBinding) :
        RecyclerView.ViewHolder(itemAttachmentBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = CollectionItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = currentList[position]
        holder.itemAttachmentBinding.apply {
            amountValue.text = currentItem.total.toString()
            collectValue.text = currentItem.collect_type
        }
        holder.itemView.rootView.clickWithDebounce {
            onClick(currentItem)
        }
    }
}
