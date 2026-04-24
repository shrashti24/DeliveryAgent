package com.source1.deliveryagent.Adapter
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.source1.deliveryagent.CallCustomerActivity
import com.source1.deliveryagent.databinding.ItemAssignedOrderBinding
import com.source1.deliveryagent.model.OrderModel

class AssignedOrderAdapter(
    private val list: List<OrderModel>,
            private val listener: OnOrderAction,

) : RecyclerView.Adapter<AssignedOrderAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAssignedOrderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAssignedOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = list[position]


        holder.binding.orderId.text = "Order #" + order.itemPushKey
        holder.binding.customerName.text = order.userName
        holder.binding.address.text = order.address
        holder.binding.status.text = order.status
        val orderId = order.itemPushKey ?: return

        holder.binding.btnAccept.setOnClickListener {
            updateStatus(orderId, "assigned")
        }

        holder.binding.btnPicked.setOnClickListener {
            updateStatus(orderId, "Picked Up")
        }

        holder.binding.btnOnWay.setOnClickListener {
            updateStatus(orderId, "On The Way")
        }
       holder.binding.btnDelivered.setOnClickListener {
            listener.onDelivered(order.itemPushKey!!, position)
        }
        holder.binding.btnReject.setOnClickListener {
            listener.onReject(orderId, position)
        }
        holder.binding.btnAccept.setOnClickListener {
            listener.onAccept(order.itemPushKey!!, position)
        }


    }
    interface OnOrderAction {
        fun onAccept(orderId: String, position: Int)
        fun onReject(orderId: String, position: Int)
        fun onDelivered(orderId: String, position: Int)
    }

    private fun updateStatus(orderId: String, status: String) {

        val ref = FirebaseDatabase.getInstance().reference

        ref.child("Orders")
            .child(orderId)
            .child("status")
            .setValue(status)
    }
}