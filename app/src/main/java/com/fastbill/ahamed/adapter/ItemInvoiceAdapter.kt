package com.fastbill.ahamed.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fastbill.ahamed.database.Invoice
import com.fastbill.ahamed.databinding.InvoiceItemBinding
import com.fastbill.ahamed.model.DiscountAction
import me.thanel.swipeactionview.SwipeActionView
import me.thanel.swipeactionview.SwipeDirection
import me.thanel.swipeactionview.SwipeGestureListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ItemInvoiceAdapter(
    private val discountList: MutableList<Invoice>,
    private val onPerformAction: (position: Int, action: DiscountAction) -> Unit
) : RecyclerView.Adapter<ItemInvoiceAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: InvoiceItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(item: Invoice) {
            binding.tvName.text = item.name
            binding.tvPrice.text = "₹${item.total}"
            if (adapterPosition == 0) {
                binding.txtDate.setText(convertTimestampToDate(item.timestamp))
                binding.relDate.setVisibility(View.VISIBLE)
            } else {
                val previousDate: String =
                    convertTimestampToDate(discountList.get(adapterPosition - 1).timestamp)
                val currentDate: String = convertTimestampToDate(item.timestamp)
                if (!previousDate.equals(currentDate, ignoreCase = true)) {
                    binding.txtDate.setText(currentDate)
                    binding.relDate.setVisibility(View.VISIBLE)
                } else {
                    binding.relDate.setVisibility(View.GONE)
                }
            }
            binding.swipeView.setDirectionEnabled(SwipeDirection.Right, false);
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(InvoiceItemBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = discountList[position]
        holder.onBind(item)
    }

    override fun getItemCount(): Int = discountList.size

    private fun convertTimestampToDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}