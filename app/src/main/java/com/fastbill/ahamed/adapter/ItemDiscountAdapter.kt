package com.fastbill.ahamed.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fastbill.ahamed.database.Discount
import com.fastbill.ahamed.databinding.ItemDiscountBinding
import com.fastbill.ahamed.model.DiscountAction
import me.thanel.swipeactionview.SwipeActionView
import me.thanel.swipeactionview.SwipeDirection
import me.thanel.swipeactionview.SwipeGestureListener
import kotlin.math.roundToInt


class ItemDiscountAdapter(
    private var discountList: List<Discount>,
    private val onPerformAction: (position: Int, action: DiscountAction) -> Unit,
    private var sum: Double
) : RecyclerView.Adapter<ItemDiscountAdapter.ViewHolder>() {

    inner class ViewHolder(val itemRowBinding: ItemDiscountBinding) :
        RecyclerView.ViewHolder(itemRowBinding.root) {
        fun onBind(item: Discount) {
            itemRowBinding.tvTitle.text = item.title
            if (item.percentage > 0) {
                itemRowBinding.tvPercentage.visibility = View.VISIBLE
                itemRowBinding.tvPercentage.text = "${item.percentage}%"
            } else {
                itemRowBinding.tvPercentage.visibility = View.INVISIBLE
            }
            
            // Task 1 Fix: Use the pre-calculated sequential amount from ViewModel
            val roundedCalculated = item.amount.roundToInt()
            
            val indianFormat = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN"))
            val formattedValue = indianFormat.format(roundedCalculated)
            
            val finalValue = "${if (!item.isPlus) "- " else ""}₹ $formattedValue"
            itemRowBinding.tvSum.text = finalValue

            if (!item.isPlus) {
                itemRowBinding.llDiscountRow.setBackgroundColor(android.graphics.Color.parseColor("#FFE5E5"))
            } else {
                itemRowBinding.llDiscountRow.setBackgroundColor(android.graphics.Color.WHITE)
            }

            itemRowBinding.swipeView.setDirectionEnabled(SwipeDirection.Left, true)
            itemRowBinding.swipeView.swipeGestureListener = object : SwipeGestureListener {
                override fun onSwipedLeft(swipeActionView: SwipeActionView): Boolean {
                    onPerformAction(adapterPosition, DiscountAction.DELETE)
                    return true
                }

                override fun onSwipedRight(swipeActionView: SwipeActionView): Boolean {
                    onPerformAction(adapterPosition, DiscountAction.EDIT)
                    return true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemDiscountBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = discountList[position]
        holder.onBind(item)
    }

    override fun getItemCount(): Int = discountList.size

    fun updateData(newList: List<Discount>, newSum: Double) {
        this.discountList = newList
        this.sum = newSum
        notifyDataSetChanged()
    }
}
