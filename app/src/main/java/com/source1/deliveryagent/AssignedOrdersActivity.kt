package com.source1.deliveryagent

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.source1.deliveryagent.Adapter.AssignedOrderAdapter
import com.source1.deliveryagent.databinding.ActivityAssignedOrdersBinding
import com.source1.deliveryagent.model.OrderModel


class AssignedOrdersActivity : AppCompatActivity(), AssignedOrderAdapter.OnOrderAction {

    private lateinit var binding: ActivityAssignedOrdersBinding
    private lateinit var list: ArrayList<OrderModel>
    private lateinit var adapter: AssignedOrderAdapter
    private lateinit var db: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var dashboardListener: ValueEventListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAssignedOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        list = ArrayList()
        adapter = AssignedOrderAdapter(list, this)


        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        db = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        loadOrders()
        loadDashboardCounts()

    }

    private fun loadDashboardCounts() {

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        val ref = FirebaseDatabase.getInstance().reference
            .child("Orders")
            .orderByChild("assignedTo")
            .equalTo(userId)

        dashboardListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                var pending = 0
                var picked = 0
                var delivered = 0

                for (snap in snapshot.children) {

                    val order = snap.getValue(OrderModel::class.java) ?: continue

                    when (order.status) {

                        "Accepted" -> pending++

                        "Picked Up", "On The Way", "Arrived" -> picked++

                        "Delivered" -> delivered++
                    }
                }

                binding.pendingCount.text = pending.toString()
                binding.pickedCount.text = picked.toString()
                binding.deliveredCount.text = delivered.toString()
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        ref.addValueEventListener(dashboardListener!!)
    }

    private fun loadOrders() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        db.child("Orders")
            .orderByChild("assignedTo")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    list.clear()

                    for (snap in snapshot.children) {

                        val order = snap.getValue(OrderModel::class.java)

                        if (order != null && order.status != "Delivered") {
                            order.itemPushKey = snap.key
                            list.add(order)
                        }
                    }

                    adapter.notifyDataSetChanged()

                    // 🔥 DEBUG
                    if (list.isEmpty()) {
                        Toast.makeText(
                            this@AssignedOrdersActivity,
                            "No Orders Found 😢", Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showRejectDialog(orderId: String, position: Int) {

        android.app.AlertDialog.Builder(this)
            .setTitle("Reject Order?")
            .setMessage("Are you sure you want to reject this order?")
            .setPositiveButton("Yes") { _, _ ->
                rejectOrder(orderId, position)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun rejectOrder(orderId: String, position: Int) {

        val db = FirebaseDatabase.getInstance().reference
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        // 🔥 ORDER RESET
        val orderUpdates = hashMapOf<String, Any>(

            "status" to "pending",
            "assignedTo" to "",
            "deliveryAccepted" to false,

            "deliveryBoyName" to "",

            "rejectedBy" to userId,
            "rejectedAt" to System.currentTimeMillis()
        )

        db.child("Orders")
            .child(orderId)
            .updateChildren(orderUpdates)

            .addOnSuccessListener {

                // 🔥 DELIVERY BOY RESET
                val deliveryBoyUpdates = hashMapOf<String, Any>(

                    "isAvailable" to true,
                    "currentOrder" to "",
                    "activeDrops" to 0
                )

                db.child("DeliveryBoys")
                    .child(userId)
                    .updateChildren(deliveryBoyUpdates)

                // 🔥 assignedOrders decrease
                db.child("DeliveryBoys")
                    .child(userId)
                    .child("assignedOrders")
                    .get()
                    .addOnSuccessListener { snap ->

                        val current =
                            snap.getValue(Int::class.java) ?: 0

                        val updated =
                            if (current > 0) current - 1 else 0

                        db.child("DeliveryBoys")
                            .child(userId)
                            .child("assignedOrders")
                            .setValue(updated)
                    }

                // remove list item
                list.removeAt(position)
                adapter.notifyItemRemoved(position)

                Toast.makeText(
                    this,
                    "Order Rejected Successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }

            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Reject Failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onDelivered(orderId: String, position: Int) {

        // 🔥 Payment screen open hoga
        val intent = Intent(this, CodPaymentActivity::class.java)
        intent.putExtra("orderId", orderId)
        startActivity(intent)
    }

    override fun onReject(orderId: String, position: Int) {
        showRejectDialog(orderId, position)
    }

    override fun onAccept(orderId: String, position: Int) {

        val db = FirebaseDatabase.getInstance().reference
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        db.child("DeliveryBoys").child(userId).updateChildren(
            mapOf(
                "isAvailable" to false,
                "currentOrder" to orderId
            )
        )
        val updates = hashMapOf<String, Any>(
            "status" to "Accepted",
            // 🔥 clear old reject data
            "rejectedBy" to "",
            "rejectedAt" to "",

            // 🔥 accepted by delivery boy
            "deliveryAccepted" to true
        )

        db.child("Orders").child(orderId)
            .updateChildren(updates)
            .addOnSuccessListener {

                Toast.makeText(this, "Order Accepted ✅", Toast.LENGTH_SHORT).show()

                // optional UI remove/update
                list[position].status = "Accepted"
                adapter.notifyItemChanged(position)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to accept ❌", Toast.LENGTH_SHORT).show()
            }
        updateOrderEverywhere(orderId, "Accepted")
    }
    private fun updateOrderEverywhere(orderId: String, status: String) {

        val ref = FirebaseDatabase.getInstance().reference

        // 1. Orders update
        ref.child("Orders")
            .child(orderId)
            .child("status")
            .setValue(status)

        // 2. Get userId
        ref.child("Orders")
            .child(orderId)
            .get()
            .addOnSuccessListener { snapshot ->

                val userId = snapshot.child("userUid").getValue(String::class.java)

                if (!userId.isNullOrEmpty()) {

                    // 3. User BuyHistory update
                    ref.child("user")
                        .child(userId)
                        .child("BuyHistory")
                        .child(orderId)
                        .child("status")
                        .setValue(status)
                }
            }
    }
    override fun onStatusChange(orderId: String, status: String, position: Int) {

        updateOrderEverywhere(orderId, status)  // 🔥 MAIN FIX

        list[position].status = status
        adapter.notifyItemChanged(position)
    }
    override fun onResume() {
        super.onResume()
        loadOrders()   // 🔥 refresh after payment
    }
    override fun onDestroy() {
        super.onDestroy()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val ref = FirebaseDatabase.getInstance().reference
            .child("Orders")
            .orderByChild("assignedTo")
            .equalTo(userId)

        dashboardListener?.let {
            ref.removeEventListener(it)
        }
    }
}