package com.fastbill.ahamed.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.fastbill.ahamed.R
import com.fastbill.ahamed.databinding.EditItemDialogBinding
import com.fastbill.ahamed.databinding.ItemRowBinding
import com.fastbill.ahamed.model.TemporaryItem
import me.thanel.swipeactionview.SwipeActionView
import me.thanel.swipeactionview.SwipeGestureListener
import java.util.Locale


class ItemAdapter(
    private val itemList: MutableList<TemporaryItem>, private val onUpdateSummary: () -> Unit
) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    inner class ViewHolder(val itemRowBinding: ItemRowBinding) :
        RecyclerView.ViewHolder(itemRowBinding.root) {
        fun onBind(item: TemporaryItem) {
            itemRowBinding.llMain.background = if (adapterPosition % 2 == 0) {
                ColorDrawable(ContextCompat.getColor(itemRowBinding.llMain.context, R.color.white))
            } else {
                ColorDrawable(
                    ContextCompat.getColor(
                        itemRowBinding.llMain.context, R.color.grey_list
                    )
                )
            }
            itemRowBinding.indexTextView.text = (adapterPosition + 1).toString()
            itemRowBinding.itemNameTextView.text = item.name
            itemRowBinding.quantityTextView.text = item.quantity.toString()

            val defaultQty = itemRowBinding.root.context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).getInt("default_quantity", 4)
            val normalSize = itemRowBinding.root.context.resources.getDimension(R.dimen._11ssp)
            val largeSize = itemRowBinding.root.context.resources.getDimension(R.dimen._13ssp)
            
            val boldTypeface = ResourcesCompat.getFont(itemRowBinding.root.context, R.font.lato_bold)
            val normalTypeface = ResourcesCompat.getFont(itemRowBinding.root.context, R.font.lato_regular)

            if (item.quantity != defaultQty) {
                itemRowBinding.quantityTextView.typeface = boldTypeface
                itemRowBinding.quantityTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, largeSize)
                itemRowBinding.quantityTextView.setTextColor(Color.parseColor("#D32F2F"))
            } else {
                itemRowBinding.quantityTextView.typeface = normalTypeface
                itemRowBinding.quantityTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, normalSize)
                itemRowBinding.quantityTextView.setTextColor(Color.BLACK)
            }

            itemRowBinding.rateTextView.text = item.rate.toInt().toString()
            itemRowBinding.totalTextView.text = String.format(Locale.US, "%.2f", item.total)

            itemRowBinding.swipeView.swipeGestureListener = object : SwipeGestureListener {
                override fun onSwipedLeft(swipeActionView: SwipeActionView): Boolean {
                    showDeleteConfirmationDialog(itemRowBinding.swipeView.context, adapterPosition)
                    return true
                }

                override fun onSwipedRight(swipeActionView: SwipeActionView): Boolean {
                    showEditDialog(adapterPosition, itemRowBinding.swipeView.context)
                    return true
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(context: Context, position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Confirm Deletion")
        builder.setMessage("Are you sure you want to delete this item?")
        builder.setPositiveButton("Yes") { _, _ ->
            removeItem(position)
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss() // Dismiss the dialog if the user cancels
        }
        builder.create().show()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemRowBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.onBind(item)
    }

    override fun getItemCount(): Int = itemList.size

    fun removeItem(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            itemList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemList.size)
            onUpdateSummary()
        }
    }

    fun editItem(position: Int, updatedItem: TemporaryItem) {
        if (position != RecyclerView.NO_POSITION) {
            itemList[position] = updatedItem
            notifyItemChanged(position)
            onUpdateSummary()
        }
    }

    private var dialog: AlertDialog? = null
    private fun showEditDialog(position: Int, context: Context) {
        val layoutInflater = LayoutInflater.from(context)
        val dialogView = EditItemDialogBinding.inflate(layoutInflater)

        val currentItem = itemList[position]
        dialogView.itemNameInput.setText(currentItem.name)
        dialogView.quantityInput.setText(currentItem.quantity.toString())
        dialogView.rateInput.setText(currentItem.rate.toString())
        dialogView.tvTotalItem.text = String.format(Locale.US, "%.2f", currentItem.total)

        dialogView.quantityInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val quantity = dialogView.quantityInput.text.toString().toIntOrNull() ?: 0
                val rate = dialogView.rateInput.text.toString().toDoubleOrNull() ?: 0.00
                val total = quantity * rate
                dialogView.tvTotalItem.text = String.format(Locale.US, "%.2f", total)
            }
        })

        dialogView.rateInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val quantity = dialogView.quantityInput.text.toString().toIntOrNull() ?: 0
                val rate = dialogView.rateInput.text.toString().toDoubleOrNull() ?: 0.00
                val total = quantity * rate
                dialogView.tvTotalItem.text = String.format(Locale.US, "%.2f", total)
            }
        })
        dialogView.quantityInput.setOnEditorActionListener { _, actionId, _ ->
            if (dialogView.rateInput.text?.isNotEmpty() == true) {
                dialogView.rateInput.selectAll()
            }
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                if (dialogView.quantityInput.text?.isNotEmpty() == true) {
                    dialogView.rateInput.requestFocus()
                    return@setOnEditorActionListener true
                } else {
                    Toast.makeText(context, "Enter Quantity!", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }
            }
            false
        }
        dialogView.rateInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (dialogView.rateInput.text?.isNotEmpty() == true) {
                    val name = dialogView.itemNameInput.text.toString().trim()
                    val quantity = dialogView.quantityInput.text.toString().toIntOrNull() ?: 0
                    val rate = dialogView.rateInput.text.toString().toDoubleOrNull() ?: 0.00
                    val total = quantity * rate

                    if (name.isNotEmpty() && quantity > 0 && rate > 0) {
                        val updatedItem = TemporaryItem(name, quantity, rate, total)
                        editItem(position, updatedItem)
                        dialog?.dismiss()
                    } else {
                        Toast.makeText(context, "Invalid input", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Enter Rate!", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }
                true // Consume the event
            } else {
                false // Let the system handle other actions
            }
        }

        val titleTextView = TextView(context).apply {
            text = "Edit Item" // Set the title text
            textSize = 18f // Set the text size
            setTextColor(Color.BLACK) // Set the text color to black
            setPadding(0, 30, 0, 10)
            typeface = ResourcesCompat.getFont(context, R.font.lato_bold)
            gravity = Gravity.CENTER // Center-align the text
        }
        dialog = AlertDialog.Builder(context).setView(dialogView.root).setCustomTitle(titleTextView)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogView.itemNameInput.text.toString().trim()
                val quantity = dialogView.quantityInput.text.toString().toIntOrNull() ?: 0
                val rate = dialogView.rateInput.text.toString().toDoubleOrNull() ?: 0.00
                val total = quantity * rate

                if (name.isNotEmpty() && quantity > 0 && rate > 0) {
                    val updatedItem = TemporaryItem(name, quantity, rate, total)
                    editItem(position, updatedItem)
                } else {
                    Toast.makeText(context, "Invalid input", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("Cancel", null).create()

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        dialog?.show()
    }
}