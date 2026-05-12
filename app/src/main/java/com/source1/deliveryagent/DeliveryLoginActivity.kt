package com.source1.deliveryagent

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.source1.deliveryagent.databinding.ActivityDeliveryLoginBinding

class DeliveryLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeliveryLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDeliveryLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        binding.loginbutton.setOnClickListener {

            val email = binding.emaillogin.text.toString().trim()
            val password = binding.passwordlogin.text.toString().trim()

            // ✅ Empty Validation
            if (email.isEmpty()) {
                binding.emaillogin.error = "Enter Email"
                binding.emaillogin.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.passwordlogin.error = "Enter Password"
                binding.passwordlogin.requestFocus()
                return@setOnClickListener
            }

            // ✅ Email Format Validation
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emaillogin.error = "Invalid Email"
                binding.emaillogin.requestFocus()
                return@setOnClickListener
            }

            // ✅ Password Length Validation
            if (password.length < 7) {
                binding.passwordlogin.error = "Password too short"
                binding.passwordlogin.requestFocus()
                return@setOnClickListener
            }

            // ✅ Firebase Login
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {

                    val userId = auth.currentUser!!.uid

                    database.child("DeliveryBoys")
                        .child(userId)
                        .child("role")
                        .addListenerForSingleValueEvent(object : ValueEventListener {

                            override fun onDataChange(snapshot: DataSnapshot) {

                                val role = snapshot.getValue(String::class.java)

                                if (role == "deliveryBoy") {

                                    startActivity(
                                        Intent(
                                            this@DeliveryLoginActivity,
                                            DeliveryAgentActivity::class.java
                                        )
                                    )

                                    finish()

                                } else {

                                    auth.signOut()

                                    Toast.makeText(
                                        this@DeliveryLoginActivity,
                                        "You are not a delivery boy!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {

                                Toast.makeText(
                                    this@DeliveryLoginActivity,
                                    "Database Error",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }

                .addOnFailureListener {

                    Toast.makeText(
                        this,
                        "Login Failed: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
        binding.forgotpassword.setOnClickListener {

            val email = binding.emaillogin.text.toString().trim()

            if (email.isEmpty()) {
                binding.emaillogin.error = "Enter Email First"
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emaillogin.error = "Invalid Email"
                return@setOnClickListener
            }
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Reset link sent to email", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
        }
        var isPasswordVisible = false

        binding.eyeIcon.setOnClickListener {

            if (isPasswordVisible) {
                binding.passwordlogin.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.eyeIcon.setImageResource(R.drawable.eye_hide)
            } else {
                binding.passwordlogin.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.eyeIcon.setImageResource(R.drawable.eye)
            }

            binding.passwordlogin.setSelection(binding.passwordlogin.text.length)
            isPasswordVisible = !isPasswordVisible
        }
    }
}