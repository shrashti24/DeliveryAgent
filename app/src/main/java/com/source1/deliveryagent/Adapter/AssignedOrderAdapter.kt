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
            listener.onAccept(order.itemPushKey!!, position)
        }

        holder.binding.btnPicked.setOnClickListener {
            listener.onStatusChange(orderId, "Picked Up", position)
        }

        holder.binding.btnOnWay.setOnClickListener {
            listener.onStatusChange(orderId, "On The Way", position)
        }
       holder.binding.btnDelivered.setOnClickListener {
            listener.onDelivered(order.itemPushKey!!, position)
        }
        holder.binding.btnArrived.setOnClickListener {
            listener.onStatusChange(orderId, "Arrived", position)
        }
        holder.binding.btnReject.setOnClickListener {
            listener.onReject(orderId, position)
        }



    }
    interface OnOrderAction {
        fun onAccept(orderId: String, position: Int)
        fun onReject(orderId: String, position: Int)
        fun onDelivered(orderId: String, position: Int)
        fun onStatusChange(orderId: String, status: String, position: Int)
    }


//    private fun updateStatus(orderId: String, status: String) {
//
//        val ref = FirebaseDatabase.getInstance().reference
//
//        // 1. Orders update
//        ref.child("Orders")
//            .child(orderId)
//            .child("status")
//            .setValue(status)
//
//        // 2. USER SIDE UPDATE 🔥🔥🔥
//        ref.child("Orders").child(orderId)
//            .child("userId")
//            .get()
//            .addOnSuccessListener { snapshot ->
//
//                val userId = snapshot.getValue(String::class.java)
//
//                if (!userId.isNullOrEmpty()) {
//                    ref.child("user")
//                        .child(userId)
//                        .child("BuyHistory")
//                        .child(orderId)
//                        .child("status")
//                        .setValue(status)
//                }
//            }
//    }
    }