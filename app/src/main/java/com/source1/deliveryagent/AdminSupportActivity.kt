package com.source1.deliveryagent

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class AdminSupportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_support)

        val back = findViewById<ImageButton>(R.id.btnBack)
        val chat = findViewById<TextView>(R.id.btnStartChat)
        val report = findViewById<TextView>(R.id.btnReportIssue)
        val reassign = findViewById<TextView>(R.id.btnReassign)

        back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }



        report.setOnClickListener {
            val ref = FirebaseDatabase.getInstance().getReference("issues")

            val issue = mapOf(
                "agentId" to "AGENT_001",
                "issueType" to "Delivery Issue",
                "description" to "Customer unreachable",
                "timestamp" to System.currentTimeMillis()
            )

            ref.push().setValue(issue)
                .addOnSuccessListener {
                    Toast.makeText(this, "Issue Reported", Toast.LENGTH_SHORT).show()
                }
        }

        reassign.setOnClickListener {
            val ref = FirebaseDatabase.getInstance().getReference("reassignment_requests")

            val request = mapOf(
                "agentId" to "AGENT_001",
                "orderId" to "ORDER_4582",
                "reason" to "Cannot deliver",
                "status" to "pending"
            )

            ref.push().setValue(request)
                .addOnSuccessListener {
                    Toast.makeText(this, "Request sent", Toast.LENGTH_SHORT).show()
                }
        }
    }
}