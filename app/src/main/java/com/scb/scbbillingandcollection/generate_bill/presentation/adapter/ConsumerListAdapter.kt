package com.scb.scbbillingandcollection.generate_bill.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.scb.scbbillingandcollection.core.extensions.clickWithDebounce
import com.scb.scbbillingandcollection.core.extensions.diffChecker
import com.scb.scbbillingandcollection.databinding.ItemCanListBinding
import com.scb.scbbillingandcollection.generate_bill.data.models.Consumers

class ConsumersListAdapter(private val onClick: (Consumers) -> Unit) :
    ListAdapter<Consumers, ConsumersListAdapter.ViewHolder>(diffChecker { oldItem, newItem -> oldItem.id == newItem.id }) {

    class ViewHolder(val itemAttachmentBinding: ItemCanListBinding) :
        RecyclerView.ViewHolder(itemAttachmentBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = ItemCanListBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = currentList[position]
        holder.itemAttachmentBinding.apply {
            phNo.text = currentItem.phone_no
            address.text = currentItem.location
            canNumber.text = currentItem.can_number
            consumerName.text = currentItem.consumer_name
            scbNo.text = currentItem.scb_no
        }
        holder.itemView.rootView.clickWithDebounce {
            onClick(currentItem)
        }
    }
}
