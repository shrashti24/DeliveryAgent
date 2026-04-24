package com.source1.deliveryagent

import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.source1.deliveryagent.databinding.ActivityCallCustomerBinding

class CallCustomerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallCustomerBinding
    private var phoneNumber: String? = null   // ✅ global variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCallCustomerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val orderId = intent.getStringExtra("orderId")

        if (orderId != null) {

            val ref = FirebaseDatabase.getInstance().reference

            ref.child("Orders")
                .child(orderId)
                .get()
                .addOnSuccessListener { orderSnap ->

                    val phone = orderSnap.child("phoneNumber").value?.toString()
                    val name = orderSnap.child("userName").value?.toString()

                    binding.customerName.text = name ?: "Customer"
                    binding.customerNumber.text = phone ?: "N/A"

                    phoneNumber = phone
                }

        } else {
            Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show()
        }

        // ✅ BUTTON CLICK (ALWAYS OUTSIDE)
        binding.btnCallNow.setOnClickListener {

            if (!phoneNumber.isNullOrEmpty()) {

                // ✅ Permission check
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED) {

                    // ✅ Direct call
                    val intent = Intent(Intent.ACTION_CALL)
                    intent.data = Uri.parse("tel:$phoneNumber")
                    startActivity(intent)

                } else {
                    // 🔥 Permission request
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CALL_PHONE),
                        1
                    )
                }

            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        }

    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // ✅ Permission mil gayi → call lagao
                if (!phoneNumber.isNullOrEmpty()) {
                    val intent = Intent(Intent.ACTION_CALL)
                    intent.data = Uri.parse("tel:$phoneNumber")
                    startActivity(intent)
                }

            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}