package com.fastbill.ahamed.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fastbill.ahamed.databinding.InvoiceItemBinding
import com.fastbill.ahamed.databinding.ItemHistoryHeaderBinding
import com.fastbill.ahamed.model.DiscountAction
import com.fastbill.ahamed.model.HistoryListItem
import me.thanel.swipeactionview.SwipeActionView
import me.thanel.swipeactionview.SwipeDirection
import me.thanel.swipeactionview.SwipeGestureListener
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ItemInvoiceAdapter(
    private val itemList: MutableList<HistoryListItem>,
    private val onPerformAction: (position: Int, action: DiscountAction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_BILL = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (itemList[position]) {
            is HistoryListItem.DateHeader -> VIEW_TYPE_HEADER
            is HistoryListItem.BillData -> VIEW_TYPE_BILL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(ItemHistoryHeaderBinding.inflate(layoutInflater, parent, false))
        } else {
            BillViewHolder(InvoiceItemBinding.inflate(layoutInflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = itemList[position]
        if (holder is HeaderViewHolder && item is HistoryListItem.DateHeader) {
            holder.bind(item)
        } else if (holder is BillViewHolder && item is HistoryListItem.BillData) {
            holder.bind(item)
        }
    }

    override fun getItemCount(): Int = itemList.size

    inner class HeaderViewHolder(private val binding: ItemHistoryHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryListItem.DateHeader) {
            val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))
            binding.tvHeaderDate.text = item.date
            binding.tvHeaderCount.text = "${item.billCount} Bills"
            binding.tvHeaderTotal.text = "₹ ${indianFormat.format(item.dailyTotal.toInt())}"
        }
    }

    inner class BillViewHolder(private val binding: InvoiceItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryListItem.BillData) {
            val bill = item.invoice
            binding.tvName.text = bill.name
            binding.tvPrice.text = "₹${bill.total}"
            
            // Hide the old inline date header as we now use the dedicated HeaderViewHolder
            binding.relDate.visibility = View.GONE

            binding.swipeView.setDirectionEnabled(SwipeDirection.Right, false)
            binding.swipeView.swipeGestureListener = object : SwipeGestureListener {
                override fun onSwipedLeft(swipeActionView: SwipeActionView): Boolean {
                    onPerformAction(adapterPosition, DiscountAction.DELETE)
                    return true
                }
                override fun onSwipedRight(swipeActionView: SwipeActionView): Boolean {
                    return true
                }
            }
            binding.relMain.setOnClickListener {
                onPerformAction(adapterPosition, DiscountAction.ACTIVATE)
            }
        }
    }
}