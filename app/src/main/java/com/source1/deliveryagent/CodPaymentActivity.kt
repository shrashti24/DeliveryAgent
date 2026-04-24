package com.source1.deliveryagent

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
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
    private val paymentAmount = 540

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cod_payment)

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
        stopTimer()
        
        // Update local earnings (simulated)
        val sharedPref = getSharedPreferences("DeliveryAgentPrefs", MODE_PRIVATE)
        val currentEarnings = sharedPref.getInt("earnings", 960) // 960 is the default from dashboard UI
        val newEarnings = currentEarnings + paymentAmount
        sharedPref.edit().putInt("earnings", newEarnings).apply()

        Toast.makeText(this, "Transaction Successful! Rs $paymentAmount added to earnings.", Toast.LENGTH_LONG).show()
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
