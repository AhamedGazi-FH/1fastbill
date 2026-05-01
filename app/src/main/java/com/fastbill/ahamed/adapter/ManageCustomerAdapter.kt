package com.fastbill.ahamed.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fastbill.ahamed.database.Customer
import com.fastbill.ahamed.databinding.RawCustomerManageItemBinding

class ManageCustomerAdapter(
    private var customers: List<Customer>,
    private val onDeleteClick: (Customer) -> Unit
) : RecyclerView.Adapter<ManageCustomerAdapter.ViewHolder>() {

    private var originalList: List<Customer> = customers

    class ViewHolder(val binding: RawCustomerManageItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RawCustomerManageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val customer = customers[position]
        holder.binding.tvCustomerName.text = customer.customerName
        holder.binding.btnDeleteCustomer.setOnClickListener {
            onDeleteClick(customer)
        }
    }

    override fun getItemCount(): Int = customers.size

    fun updateData(newCustomers: List<Customer>) {
        originalList = newCustomers
        customers = newCustomers
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        customers = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter {
                it.customerName.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }
}
