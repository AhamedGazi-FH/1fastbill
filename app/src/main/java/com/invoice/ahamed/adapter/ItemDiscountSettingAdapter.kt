package com.invoice.ahamed.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemLongClickListener
import androidx.recyclerview.widget.RecyclerView
import com.github.angads25.toggle.interfaces.OnToggledListener
import com.github.angads25.toggle.model.ToggleableView
import com.github.angads25.toggle.widget.LabeledSwitch
import com.invoice.ahamed.database.Discount
import com.invoice.ahamed.databinding.RawDiscountItemBinding
import com.invoice.ahamed.model.DiscountAction
import me.thanel.swipeactionview.SwipeActionView
import me.thanel.swipeactionview.SwipeDirection
import me.thanel.swipeactionview.SwipeGestureListener
import java.util.Locale


class ItemDiscountSettingAdapter(
    private val discountList: MutableList<Discount>,
    private val onPerformAction: (position: Int, action: DiscountAction) -> Unit
) : RecyclerView.Adapter<ItemDiscountSettingAdapter.ViewHolder>() {

    inner class ViewHolder(val itemRowBinding: RawDiscountItemBinding) :
        RecyclerView.ViewHolder(itemRowBinding.root) {
        fun onBind(item: Discount) {
            itemRowBinding.tvTitle.text = item.title
            itemRowBinding.isActive.isOn = item.isActive
            itemRowBinding.isActive.setOnToggledListener(object : OnToggledListener {
                override fun onSwitched(toggleableView: ToggleableView?, isOn: Boolean) {
                    onPerformAction(adapterPosition, DiscountAction.ACTIVATE)
                }
            })
            itemRowBinding.swipeView.setOnLongClickListener {
                onPerformAction(adapterPosition, DiscountAction.DELETE)
                true
            }
//            itemRowBinding.swipeView.swipeGestureListener = object : SwipeGestureListener {
//                override fun onSwipedLeft(swipeActionView: SwipeActionView): Boolean {
//                    onPerformAction(adapterPosition, DiscountAction.DELETE)
//                    return true
//                }
//
//                override fun onSwipedRight(swipeActionView: SwipeActionView): Boolean {
//                    onPerformAction(adapterPosition, DiscountAction.EDIT)
//                    return true
//                }
//            }
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