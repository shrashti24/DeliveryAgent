package com.source1.deliveryagent

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.util.Log
import android.view.View
import com.google.firebase.database.Transaction
import com.google.firebase.database.MutableData
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class CodPaymentActivity : AppCompatActivity() {

    private lateinit var sectionCash: LinearLayout
    private lateinit var sectionUPI: LinearLayout
    private lateinit var btnCash: LinearLayout
    private lateinit var btnUPI: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var btnConfirmCash: TextView
    private lateinit var btnConfirmUPI: TextView
    private lateinit var btnViewEarnings: TextView
    private lateinit var ivQRCode: ImageView
    private lateinit var tvTimer: TextView
    private lateinit var tvTotalAmount: TextView
    
    private var ivCashIcon: ImageView? = null
    private var tvCashText: TextView? = null
    private var ivUPIIcon: ImageView? = null
    private var tvUPIText: TextView? = null

    private val client = OkHttpClient()
    private val apiKey = "rzp_test_SfgOOm510YFFzO"
    private val apiSecret = "yCAtO5rWa4nLX0iaFqix7VV3"
    
    private var countDownTimer: CountDownTimer? = null
    private var orderId: String? = null
    private lateinit var database: DatabaseReference
    private var paymentAmount = 0
    private lateinit var tvOrderInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cod_payment)
        database = FirebaseDatabase.getInstance().reference
        orderId = intent.getStringExtra("orderId")
        if (orderId.isNullOrEmpty()) {
            Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        // Initialize Views
        sectionCash = findViewById(R.id.sectionCash)
        sectionUPI = findViewById(R.id.sectionUPI)
        btnCash = findViewById(R.id.btnCash)
        btnUPI = findViewById(R.id.btnUPI)
        btnBack = findViewById(R.id.btnBack)
        btnConfirmCash = findViewById(R.id.btnConfirmCash)
        btnConfirmUPI = findViewById(R.id.btnConfirmUPI)
        btnViewEarnings = findViewById(R.id.btnViewEarnings)
        ivQRCode = findViewById(R.id.ivQRCode)
        tvTimer = findViewById(R.id.tvTimer)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvOrderInfo = findViewById(R.id.tvOrderInfo)

        // Mode Icons and Texts safely
        if (btnCash.childCount >= 2) {
            ivCashIcon = btnCash.getChildAt(0) as? ImageView
            tvCashText = btnCash.getChildAt(1) as? TextView
        }
        if (btnUPI.childCount >= 2) {
            ivUPIIcon = btnUPI.getChildAt(0) as? ImageView
            tvUPIText = btnUPI.getChildAt(1) as? TextView
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnCash.setOnClickListener {
            stopTimer()
            selectPaymentMode(isCash = true)
        }

        btnUPI.setOnClickListener {
            selectPaymentMode(isCash = false)
            generateRazorpayQRCode(paymentAmount)
            startTimer()
        }

        btnConfirmCash.setOnClickListener {
            processSuccessfulPayment()
        }

        btnConfirmUPI.setOnClickListener {
            processSuccessfulPayment()
        }

        btnViewEarnings.setOnClickListener {
            startActivity(Intent(this, EarningsSummaryActivity::class.java))
        }
        fetchOrderAmount()

    }

    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvTimer.text = "Code valid until: ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                tvTimer.text = "Code expired"
                btnConfirmUPI.isEnabled = false
                btnConfirmUPI.alpha = 0.5f
                Toast.makeText(this@CodPaymentActivity, "Unsuccessful transaction: Payment timed out", Toast.LENGTH_LONG).show()
            }
        }.start()
        
        btnConfirmUPI.isEnabled = true
        btnConfirmUPI.alpha = 1.0f
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
    }

    private fun processSuccessfulPayment() {

        if (orderId == null) return

        val deliveryBoyId = FirebaseAuth.getInstance().currentUser!!.uid

        // ✅ Step 1: Update Orders + CompletedOrder (ONE TIME ONLY)
        val updates = mapOf(
            "paymentReceived" to true,
            "status" to "Delivered"
        )

        database.child("Orders").child(orderId!!).updateChildren(updates)


        // ✅ Step 2: Sync User BuyHistory (VERY IMPORTANT)
        val orderRef = database.child("Orders").child(orderId!!)

        orderRef.get().addOnSuccessListener { snapshot ->

            val orderMap = HashMap<String, Any>()

            orderMap["orderId"] = orderId!!
            orderMap["userName"] = snapshot.child("userName").value ?: ""
            orderMap["address"] = snapshot.child("address").value ?: ""
            orderMap["phoneNumber"] = snapshot.child("phoneNumber").value ?: ""
            orderMap["totalPrice"] = snapshot.child("totalPrice").value ?: "0"
            orderMap["userUid"] = snapshot.child("userUid").value ?: ""
            orderMap["status"] = "Delivered"
            orderMap["paymentReceived"] = true

            // 🔥 NOW SAVE PROPERLY
            database.child("CompletedOrder")
                .child(orderId!!)
                .setValue(orderMap)
        }

        orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val userIdFromOrder = snapshot.child("userUid").getValue(String::class.java)

                if (!userIdFromOrder.isNullOrEmpty()) {

                    val userUpdates = mapOf(
                        "paymentReceived" to true,
                        "status" to "Delivered"   // 🔥 ADD THIS
                    )


                    database.child("user")
                        .child(userIdFromOrder)
                        .child("BuyHistory")
                        .child(orderId!!)
                        .updateChildren(userUpdates)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // ✅ Step 3: Delivery Boy Reset
        val boyRef = database.child("DeliveryBoys").child(deliveryBoyId)

        boyRef.updateChildren(
            mapOf(
                "isAvailable" to true,
                "currentOrder" to ""   // 🔥 clear order after payment
            )
        )

        // ✅ Step 4: Update Earnings
        database.child("Orders").child(orderId!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val amountStr = snapshot.child("totalPrice")
                        .getValue(String::class.java) ?: "0"

                    val amount = amountStr.replace("₹", "").toIntOrNull() ?: 0

                    updateDeliveryBoyEarnings(deliveryBoyId, amount)
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show()
    }


    private fun fetchOrderAmount() {

        database.child("Orders").child(orderId!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val amountStr = snapshot.child("totalPrice")
                        .getValue(String::class.java) ?: "0"

                    paymentAmount = amountStr.replace("₹", "").toIntOrNull() ?: 0

                    tvTotalAmount.text = "Rs $paymentAmount"

                    // 🔹 Order ID
                    val orderIdText = snapshot.key ?: ""

                    // 🔹 CUSTOMER NAME (IMPORTANT 🔥)
                    val customerName = snapshot.child("userName")
                        .getValue(String::class.java) ?: "Customer"

                    // ✅ FINAL TEXT
                    tvOrderInfo.text = "Order #$orderIdText | $customerName"
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
    private fun updateDeliveryBoyEarnings(userId: String, amount: Int) {

        val ref = database.child("DeliveryBoys").child(userId)

        ref.runTransaction(object : Transaction.Handler {

            override fun doTransaction(currentData: MutableData): Transaction.Result {

                val currentEarnings = currentData.child("earnings").getValue(Int::class.java) ?: 0
                val delivered = currentData.child("deliveredOrders").getValue(Int::class.java) ?: 0
                val active = currentData.child("activeDrops").getValue(Int::class.java) ?: 0

                currentData.child("earnings").value = currentEarnings + amount
                currentData.child("deliveredOrders").value = delivered + 1
                currentData.child("activeDrops").value = if (active > 0) active - 1 else 0

                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (committed) {
                    Toast.makeText(this@CodPaymentActivity, "Earnings Updated!", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun selectPaymentMode(isCash: Boolean) {
        if (isCash) {
            sectionCash.visibility = View.VISIBLE
            sectionUPI.visibility = View.GONE
            btnCash.setBackgroundResource(R.drawable.delivery_status_chip)
            ivCashIcon?.setColorFilter(ContextCompat.getColor(this, R.color.green))
            tvCashText?.setTextColor(ContextCompat.getColor(this, R.color.green))
            btnUPI.setBackgroundResource(0)
            ivUPIIcon?.setColorFilter(ContextCompat.getColor(this, R.color.textColor))
            tvUPIText?.setTextColor(ContextCompat.getColor(this, R.color.textColor))
        } else {
            sectionCash.visibility = View.GONE
            sectionUPI.visibility = View.VISIBLE
            btnUPI.setBackgroundResource(R.drawable.delivery_status_chip)
            ivUPIIcon?.setColorFilter(ContextCompat.getColor(this, R.color.green))
            tvUPIText?.setTextColor(ContextCompat.getColor(this, R.color.green))
            btnCash.setBackgroundResource(0)
            ivCashIcon?.setColorFilter(ContextCompat.getColor(this, R.color.textColor))
            tvCashText?.setTextColor(ContextCompat.getColor(this, R.color.textColor))
        }
    }

    private fun generateRazorpayQRCode(amount: Int) {
        val amountInPaise = amount * 100
        val json = JSONObject().apply {
            put("type", "upi_qr")
            put("name", "Store Payment")
            put("usage", "single_use")
            put("fixed_amount", true)
            put("payment_amount", amountInPaise)
            put("description", "Payment for Order #4582")
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val auth = Base64.encodeToString("$apiKey:$apiSecret".toByteArray(), Base64.NO_WRAP)

        val request = Request.Builder()
            .url("https://api.razorpay.com/v1/payments/qr_codes")
            .post(requestBody)
            .addHeader("Authorization", "Basic $auth")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CodPaymentActivity, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val qrImageUrl = jsonResponse.optString("image_url")
                        runOnUiThread {
                            if (qrImageUrl.isNotEmpty()) {
                                Glide.with(this@CodPaymentActivity)
                                    .load(qrImageUrl)
                                    .placeholder(R.drawable.logo1)
                                    .into(ivQRCode)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Razorpay", "Parsing Error", e)
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }
}
