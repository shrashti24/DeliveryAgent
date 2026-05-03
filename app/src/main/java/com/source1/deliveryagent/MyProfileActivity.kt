package com.source1.deliveryagent

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.DatabaseReference
import com.source1.deliveryagent.databinding.ActivityMyProfileBinding

class MyProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyProfileBinding
    private lateinit var db: DatabaseReference

    private val uid = FirebaseAuth.getInstance().currentUser!!.uid
    private val PICK_IMAGE = 100
    private var imageType = ""
    private var isEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseDatabase.getInstance()
            .reference.child("DeliveryBoys").child(uid)

        loadProfile()

        setEditMode(false)

        binding.btnEditProfile.setOnClickListener {
            isEditing = !isEditing

            if (isEditing) {
                setEditMode(true)
                binding.btnEditProfile.setImageResource(R.drawable.edit) // slash OFF
            } else {
                setEditMode(false)
                binding.btnEditProfile.setImageResource(R.drawable.edit_off) // slash ON
            }
        }
        binding.profileImage.setOnClickListener {
            if (!isEditing) return@setOnClickListener
            imageType = "profileImage"
            openGallery()
        }
        binding.uploadLicense.setOnClickListener {
            if (!isEditing) return@setOnClickListener
            imageType = "licenseUrl"
            openGallery()
        }


        binding.uploaRC.setOnClickListener {
            if (!isEditing) return@setOnClickListener
            imageType = "rcUrl"
            openGallery()
        }

        binding.uploaAadhar.setOnClickListener {
            if (!isEditing) return@setOnClickListener
            imageType = "aadharUrl"
            openGallery()
        }
        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)

        intent.addCategory(Intent.CATEGORY_OPENABLE)

        intent.type = "image/*"   // ✅ ONLY IMAGES

        intent.putExtra(
            Intent.EXTRA_MIME_TYPES,
            arrayOf(
                "image/jpeg",
                "image/png"
            )
        )

        startActivityForResult(intent, PICK_IMAGE)
    }
    // 🔥 LOAD DATA FROM FIREBASE
    private fun loadProfile() {

        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (!snapshot.exists()) return

                val profileImg = snapshot.child("profileImage").value?.toString()

                if (!profileImg.isNullOrEmpty()) {

                    // 🔥 IMPORTANT: old avatar remove + new set
                    Glide.with(this@MyProfileActivity)
                        .load(profileImg)
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .circleCrop()
                        .into(binding.profileImage)
                } else {
                    binding.profileImage.setImageResource(R.drawable.ic_person)
                }

                binding.tvName.text =
                    snapshot.child("name").value?.toString() ?: "Delivery Boy"



                binding.etZone.setText(
                    snapshot.child("zone").value?.toString() ?: ""
                )

                binding.etPhone.setText(snapshot.child("phone").value?.toString() ?: "")
                binding.etVehicle.setText(snapshot.child("vehicleNumber").value?.toString() ?: "")
                binding.etLicense.setText(snapshot.child("licenseId").value?.toString() ?: "")
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🔥 ENABLE / DISABLE EDIT MODE
    private fun setEditMode(enable: Boolean) {
        binding.etPhone.isEnabled = enable
        binding.etVehicle.isEnabled = enable
        binding.etLicense.isEnabled = enable
        binding.etZone.isEnabled = enable
        binding.uploadLicense.isEnabled = enable
        binding.uploaRC.isEnabled = enable
        binding.uploaAadhar.isEnabled = enable
        binding.profileImage.isEnabled = enable

        binding.btnSave.visibility = if (enable) View.VISIBLE else View.GONE
    }

    // 🔥 SAVE DATA TO FIREBASE
    private fun saveProfile() {

        val phone = binding.etPhone.text.toString()
        val vehicle = binding.etVehicle.text.toString()
        val license = binding.etLicense.text.toString()
        val zone = binding.etZone.text.toString()
        if (phone.isEmpty() || vehicle.isEmpty() || license.isEmpty()) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show()
            return
        }
        val updates = hashMapOf<String, Any>(
            "phone" to phone,
            "vehicleNumber" to vehicle,
            "licenseId" to license,
            "zone" to zone
        )
        db.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show()
            isEditing = false
            setEditMode(false)
            binding.btnEditProfile.setImageResource(R.drawable.edit_off)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                uploadToCloudinary(uri)
            }
        }
    }

    private fun uploadToCloudinary(uri: Uri) {

        val user = FirebaseAuth.getInstance().currentUser ?: return

        val inputStream = contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Toast.makeText(this, "File not readable", Toast.LENGTH_SHORT).show()
            return
        }

        val bytes = inputStream.readBytes()
        inputStream.close()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "file_${System.currentTimeMillis()}",
                bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .addFormDataPart("upload_preset", "delivery_upload")
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/dgnacg3bg/upload")
            .post(requestBody)
            .build()

        val client = OkHttpClient()

        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MyProfileActivity,
                        "Upload Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {

                val body = response.body?.string()

                if (!response.isSuccessful || body.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@MyProfileActivity,
                            "Upload Failed: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                try {
                    val json = JSONObject(body)
                    val url = json.getString("secure_url")
                    runOnUiThread {
                        db.child(imageType).setValue(url)
                        when (imageType) {
                            "profileImage" -> {
                                Glide.with(this@MyProfileActivity)
                                    .load(url)
                                    .into(binding.profileImage)
                            }

                            "licenseId" -> Toast.makeText(this@MyProfileActivity,
                                "License Uploaded", Toast.LENGTH_SHORT).show()

                            "rcUrl" -> Toast.makeText(this@MyProfileActivity,
                                "RC Uploaded", Toast.LENGTH_SHORT).show()

                            "aadharUrl" -> Toast.makeText(this@MyProfileActivity,
                                "Aadhar Uploaded", Toast.LENGTH_SHORT).show()
                        }

                        Toast.makeText(this@MyProfileActivity,
                            "Upload Success", Toast.LENGTH_SHORT).show()

                        imageType = ""
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MyProfileActivity,
                            "Parse Error: ${body}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })   // 👈 THIS CLOSES enqueue
    }
}