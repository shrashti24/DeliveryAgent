package com.source1.deliveryagent

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.source1.deliveryagent.databinding.ActivityDeliveryAgentBinding

class DeliveryAgentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeliveryAgentBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDeliveryAgentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val userId = auth.currentUser!!.uid

        loadUserData(userId)
        loadDeliveryData(userId)
        loadDeliveryBoyDashboard()
        setupClicks()
    }

    // 🔥 USER DATA
    private fun loadUserData(userId: String) {

        database.child("Users").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val name = snapshot.child("name").getValue(String::class.java)
                    val isAvailable = snapshot.child("isAvailable")
                        .getValue(Boolean::class.java) ?: true

                    binding.agentName.text = name ?: "Delivery Agent"

                    if (isAvailable == true) {
                        binding.switchDuty.text = "On Duty"
                    } else {
                        binding.switchDuty.text = "Off Duty"
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // 🔥 DELIVERY DATA
    private fun loadDeliveryData(userId: String) {

        database.child("DeliveryBoys").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val assigned = snapshot.child("assignedOrders").getValue(Int::class.java) ?: 0
                    val delivered = snapshot.child("deliveredOrders").getValue(Int::class.java) ?: 0
                    val active = snapshot.child("activeDrops").getValue(Int::class.java) ?: 0
                    val cod = snapshot.child("codEnabled").getValue(Boolean::class.java) ?: false

                    binding.assignedCount.text = assigned.toString()
                    binding.deliveredCount.text = delivered.toString()


                    binding.agentInfo.text =
                        "$active active drops | ${if (cod) "COD enabled" else "COD off"}"
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadDeliveryBoyDashboard() {

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        FirebaseDatabase.getInstance().reference
            .child("DeliveryBoys")
            .child(userId)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val name = snapshot.child("name")
                        .getValue(String::class.java) ?: "Delivery Boy"

                    val activeDrops = snapshot.child("activeDrops")
                        .getValue(Int::class.java) ?: 0

                    val deliveredOrders = snapshot.child("deliveredOrders")
                        .getValue(Int::class.java) ?: 0

                    val totalEarnings = snapshot.child("earnings")
                        .getValue(Int::class.java) ?: 0

                    binding.earningAmount.text = "₹$totalEarnings"
                    // 🔥 UI MATCH WITH YOUR XML
                    binding.agentName.text = name
                    binding.agentInfo.text = "$activeDrops active drops"

                    binding.assignedCount.text = activeDrops.toString()
                    binding.deliveredCount.text = deliveredOrders.toString()

                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }



    // 🔥 CLICK EVENTS
    private fun setupClicks() {

        val userId = auth.currentUser!!.uid

        // 🔄 DUTY TOGGLE
        binding.switchDuty.setOnClickListener {

            database.child("Users").child(userId).child("isAvailable")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {

                        val current = snapshot.getValue(Boolean::class.java) ?: true
                        database.child("Users").child(userId).child("isAvailable")
                            .setValue(!current)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        // 📦 ASSIGNED ORDERS
        binding.cardAssignedOrders.setOnClickListener {
            startActivity(Intent(this, AssignedOrdersActivity::class.java))
        }
    binding.profile.setOnClickListener {
            startActivity(Intent(this, MyProfileActivity::class.java))
        }
        binding.mapRoute.setOnClickListener {

            startActivity(Intent(this, MapsRoutesActivity::class.java))
        }
        binding.payment.setOnClickListener {
            startActivity(Intent(this, CodPaymentActivity::class.java))
        }
        binding.customercallBtn.setOnClickListener {

            val userId = auth.currentUser!!.uid

            database.child("Orders")
                .get()
                .addOnSuccessListener { snapshot ->

                    if (snapshot.exists()) {

                        for (orderSnap in snapshot.children) {

                            val assignedTo = orderSnap.child("assignedTo").value.toString()

                            if (assignedTo == userId) {

                                val orderId = orderSnap.key

                                val intent = Intent(this, CallCustomerActivity::class.java)
                                intent.putExtra("orderId", orderId)
                                startActivity(intent)
                                return@addOnSuccessListener
                            }
                        }

                        Toast.makeText(this, "No matching order", Toast.LENGTH_SHORT).show()

                    } else {
                        Toast.makeText(this, "No orders found", Toast.LENGTH_SHORT).show()
                    }
                }
        }

    }
}