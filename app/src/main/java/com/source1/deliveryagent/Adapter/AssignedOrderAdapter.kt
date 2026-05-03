package com.source1.deliveryagent.Adapter
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast

import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.source1.deliveryagent.CallCustomerActivity
import com.source1.deliveryagent.CodPaymentActivity
import com.source1.deliveryagent.MapsRoutesActivity
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


        // 🔥 UI CONTROL BASED ON STATUS

        if (order.status == "Accepted" || order.status == "Picked Up" || order.status == "On The Way" || order.status == "Arrived") {

            holder.binding.statusSection.visibility = ViewGroup.VISIBLE
            holder.binding.acceptRejectLayout.visibility = ViewGroup.GONE

        } else {

            holder.binding.statusSection.visibility = ViewGroup.GONE
            holder.binding.acceptRejectLayout.visibility = ViewGroup.VISIBLE
        }
        holder.binding.btnAccept.setOnClickListener {
            listener.onAccept(order.itemPushKey!!, position)
        }

        holder.binding.btnPicked.setOnClickListener {
            listener.onStatusChange(orderId, "Picked Up", position)
        }

        holder.binding.btnOnWay.setOnClickListener {

            val context = holder.itemView.context
            val otp = (1000..9999).random().toString()

            val ref = FirebaseDatabase.getInstance().reference

            // 🔥 SAVE OTP IN FIREBASE
            ref.child("Orders").child(orderId)
                .child("deliveryOTP")
                .setValue(otp)
            // ✅ 2. USER SIDE me bhi save (IMPORTANT 🔥)
            val userId = order.userUid   // already hai tumhare model me

            if (!userId.isNullOrEmpty()) {
                ref.child("user")
                    .child(userId)
                    .child("BuyHistory")
                    .child(orderId)
                    .child("deliveryOTP")
                    .setValue(otp)
            }
            Toast.makeText(context, "OTP Generated: $otp", Toast.LENGTH_SHORT).show()
            val intent = Intent(context, MapsRoutesActivity::class.java)
            intent.putExtra("orderId", orderId)

            context.startActivity(intent)

            listener.onStatusChange(orderId, "On The Way", position)
        }
        holder.binding.btnDelivered.setOnClickListener {

            val context = holder.itemView.context
            val ref = FirebaseDatabase.getInstance().reference

            ref.child("Orders").child(orderId).child("deliveryOTP")
                .get()
                .addOnSuccessListener { snapshot ->

                    val firebaseOtp = snapshot.getValue(String::class.java)

                    val editText = android.widget.EditText(context)
                    editText.hint = "Enter OTP"

                    android.app.AlertDialog.Builder(context)
                        .setTitle("Enter Customer OTP")
                        .setView(editText)
                        .setPositiveButton("Verify") { _, _ ->

                            val enteredOtp = editText.text.toString()

                            if (enteredOtp == firebaseOtp) {

                                // ✅ OTP correct → NOW OPEN PAYMENT
                                val intent = Intent(context, CodPaymentActivity::class.java)
                                intent.putExtra("orderId", orderId)
                                context.startActivity(intent)

                            } else {
                                Toast.makeText(context, "Wrong OTP ❌", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
        }
        holder.binding.btnArrived.setOnClickListener {

            val context = holder.itemView.context

            val intent = Intent(context, CallCustomerActivity::class.java)
            intent.putExtra("orderId", orderId)   // 🔥 IMPORTANT

            context.startActivity(intent)

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