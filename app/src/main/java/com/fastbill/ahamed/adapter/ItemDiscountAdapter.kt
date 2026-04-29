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
import java.util.Collections
import java.util.Locale
import kotlin.math.roundToInt


class ItemDiscountAdapter(
    private val discountList: MutableList<Discount>,
    private val onPerformAction: (position: Int, action: DiscountAction) -> Unit,
    private var sum: Double
) : RecyclerView.Adapter<ItemDiscountAdapter.ViewHolder>() {

    inner class ViewHolder(val itemRowBinding: ItemDiscountBinding) :
        RecyclerView.ViewHolder(itemRowBinding.root) {
        fun onBind(item: Discount) {
            itemRowBinding.tvTitle.text = item.title
            if (item.percentage > 0) {
                itemRowBinding.tvPercentage.visibility= View.VISIBLE
                itemRowBinding.tvPercentage.text = "${item.percentage}%"
            }else{
                itemRowBinding.tvPercentage.visibility= View.INVISIBLE
            }
            val calculatedValue = if (item.percentage > 0) {
                sum * (item.percentage / 100.0)
            } else {
                item.price
            }
// Update the sum based on whether it's addition or subtraction
            sum += if (item.isPlus) calculatedValue else - calculatedValue

            val roundedCalculated = calculatedValue.roundToInt()
            
            // Use Indian formatter for commas, no decimals
            val indianFormat = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN"))
            val formattedValue = indianFormat.format(roundedCalculated)
            
            val finalValue = "${if (!item.isPlus) "- " else ""}₹ $formattedValue"
            itemRowBinding.tvSum.text = finalValue

            // Apply the light red background directly to the inner layout
            if (!item.isPlus) {
                itemRowBinding.llDiscountRow.setBackgroundColor(android.graphics.Color.parseColor("#FFE5E5"))
            } else {
                itemRowBinding.llDiscountRow.setBackgroundColor(android.graphics.Color.WHITE)
            }

            itemRowBinding.swipeView.setDirectionEnabled(SwipeDirection.Left, false);
            itemRowBinding.swipeView.swipeGestureListener = object : SwipeGestureListener {
                override fun onSwipedLeft(swipeActionView: SwipeActionView): Boolean {
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

    fun updateSum(updatedSum: Double) {
        sum = updatedSum
    }
    // Method to swap items in the list
    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(discountList, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }
}