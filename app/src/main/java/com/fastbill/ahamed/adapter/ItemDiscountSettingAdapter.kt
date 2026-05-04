package com.fastbill.ahamed.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fastbill.ahamed.database.Discount
import com.fastbill.ahamed.databinding.RawDiscountItemBinding
import com.fastbill.ahamed.model.DiscountAction
import com.github.angads25.toggle.interfaces.OnToggledListener
import com.github.angads25.toggle.model.ToggleableView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ItemDiscountSettingAdapter(
    private val discountList: MutableList<Discount>,
    private val onPerformAction: (position: Int, action: DiscountAction) -> Unit
) : RecyclerView.Adapter<ItemDiscountSettingAdapter.ViewHolder>() {

    inner class ViewHolder(val itemRowBinding: RawDiscountItemBinding) :
        RecyclerView.ViewHolder(itemRowBinding.root) {
        fun onBind(item: Discount) {
            itemRowBinding.tvTitle.text = item.title
            itemRowBinding.isActive.isOn = item.isActive
            
            // Format value display (Task 3)
            val amountStr = if (item.percentage > 0) "${item.percentage}%" else "₹${item.price}"
            if (item.isPlus) {
                itemRowBinding.tvValue.text = "+ $amountStr"
                itemRowBinding.tvValue.setTextColor(Color.parseColor("#388E3C"))
            } else {
                itemRowBinding.tvValue.text = "- $amountStr"
                itemRowBinding.tvValue.setTextColor(Color.parseColor("#D32F2F"))
            }

            itemRowBinding.isActive.setOnToggledListener(object : OnToggledListener {
                override fun onSwitched(toggleableView: ToggleableView?, isOn: Boolean) {
                    onPerformAction(adapterPosition, DiscountAction.ACTIVATE)
                }
            })

            // Destructive Long-Press Fix (Task 2)
            itemRowBinding.swipeView.setOnLongClickListener {
                val context = it.context
                MaterialAlertDialogBuilder(context)
                    .setTitle("Manage Discount")
                    .setMessage("What would you like to do with '${item.title}'?")
                    .setPositiveButton("Edit") { _, _ ->
                        onPerformAction(adapterPosition, DiscountAction.EDIT)
                    }
                    .setNegativeButton("Delete") { _, _ ->
                        onPerformAction(adapterPosition, DiscountAction.DELETE)
                    }
                    .setNeutralButton("Cancel", null)
                    .show()
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(RawDiscountItemBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = discountList[position]
        holder.onBind(item)
    }

    override fun getItemCount(): Int = discountList.size
}