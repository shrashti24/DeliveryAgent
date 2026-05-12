package com.source1.deliveryagent

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.source1.deliveryagent.databinding.ActivityMyProfileBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MyProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyProfileBinding
    private lateinit var db: DatabaseReference

    private val uid = FirebaseAuth.getInstance().currentUser!!.uid

    private var imageType = ""
    private var isEditing = false
    private var isUploading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseDatabase.getInstance()
            .reference.child("DeliveryBoys").child(uid)

        setupSpinners()
        loadProfile()
        setEditMode(false)

        // ================= EDIT =================
        binding.btnEditProfile.setOnClickListener {

            isEditing = !isEditing

            setEditMode(isEditing)

            if (isEditing) {
                binding.btnEditProfile.setImageResource(R.drawable.edit)
                Toast.makeText(this, "Edit Mode Enabled", Toast.LENGTH_SHORT).show()
            } else {
                binding.btnEditProfile.setImageResource(R.drawable.edit_off)
                Toast.makeText(this, "Edit Mode Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // ================= VIEW DOCUMENTS =================

        binding.viewLicense.setOnClickListener {
            openDocument("licenseUrl")
        }

        binding.viewRC.setOnClickListener {
            openDocument("rcUrl")
        }

        binding.viewAadhar.setOnClickListener {
            openDocument("aadharUrl")
        }

        // ================= IMAGE PICKERS =================

        binding.profileImage.setOnClickListener {

            if (!isEditing) {
                Toast.makeText(this, "Enable Edit Mode First", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            imageType = "profileImage"
            openGallery()
        }

        binding.uploadLicense.setOnClickListener {

            if (!isEditing) {
                Toast.makeText(this, "Enable Edit Mode First", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            imageType = "licenseUrl"
            openGallery()
        }

        binding.uploadRC.setOnClickListener {

            if (!isEditing) {
                Toast.makeText(this, "Enable Edit Mode First", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            imageType = "rcUrl"
            openGallery()
        }

        binding.uploadAadhar.setOnClickListener {

            if (!isEditing) {
                Toast.makeText(this, "Enable Edit Mode First", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            imageType = "aadharUrl"
            openGallery()
        }

        // ================= SAVE =================

        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        // ================= BACK =================

        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    // ================= SPINNER =================

    private fun setupSpinners() {

        val vehicleTypes = arrayOf(
            "Select Vehicle Type",
            "Bike",
            "Scooty"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            vehicleTypes
        )

        binding.spinnerVehicleType.adapter = adapter
    }

    // ================= OPEN DOCUMENT =================

    private fun openDocument(child: String) {

        db.child(child).get().addOnSuccessListener { snapshot ->

            val url = snapshot.value?.toString()

            if (!url.isNullOrEmpty()) {

                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(url), "image/*")
                startActivity(intent)

            } else {

                Toast.makeText(this, "File Not Uploaded", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ================= LOAD PROFILE =================

    private fun loadProfile() {

        db.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if (!snapshot.exists()) return

                val profileImg =
                    snapshot.child("profileImage").value?.toString()

                if (!profileImg.isNullOrEmpty()) {

                    Glide.with(this@MyProfileActivity)
                        .load(profileImg)
                        .circleCrop()
                        .into(binding.profileImage)
                }

                binding.tvName.text =
                    snapshot.child("name").value?.toString() ?: "Delivery Boy"

                binding.etPhone.setText(snapshot.child("phone").value?.toString() ?: "")
                binding.etVehicle.setText(snapshot.child("vehicleNumber").value?.toString() ?: "")
                binding.etLicense.setText(snapshot.child("licenseId").value?.toString() ?: "")
                binding.etHouseNo.setText(snapshot.child("houseNo").value?.toString() ?: "")
                binding.etStreet.setText(snapshot.child("street").value?.toString() ?: "")
                binding.etPincode.setText(snapshot.child("pincode").value?.toString() ?: "")
                binding.etState.setText(snapshot.child("state").value?.toString() ?: "")
                binding.etCity.setText(snapshot.child("city").value?.toString() ?: "")

                binding.licenseFileName.text =
                    if (snapshot.child("licenseUrl").exists())
                        "License Uploaded"
                    else
                        "No License Uploaded"

                binding.rcFileName.text =
                    if (snapshot.child("rcUrl").exists())
                        "RC Uploaded"
                    else
                        "No RC Uploaded"

                binding.aadharFileName.text =
                    if (snapshot.child("aadharUrl").exists())
                        "Aadhar Uploaded"
                    else
                        "No Aadhar Uploaded"
            }

            override fun onCancelled(error: DatabaseError) {

                Toast.makeText(
                    this@MyProfileActivity,
                    error.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // ================= EDIT MODE =================

    private fun setEditMode(enable: Boolean) {

        binding.etPhone.isEnabled = enable
        binding.etVehicle.isEnabled = enable
        binding.etLicense.isEnabled = enable
        binding.etHouseNo.isEnabled = enable
        binding.etStreet.isEnabled = enable
        binding.etPincode.isEnabled = enable
        binding.etState.isEnabled = enable
        binding.etCity.isEnabled = enable
        binding.spinnerVehicleType.isEnabled = enable

        binding.uploadLicense.isEnabled = enable
        binding.uploadRC.isEnabled = enable
        binding.uploadAadhar.isEnabled = enable
        binding.profileImage.isEnabled = enable

        binding.btnSave.visibility =
            if (enable) View.VISIBLE else View.GONE
    }

    // ================= SAVE PROFILE =================

    private fun saveProfile() {

        val phone = binding.etPhone.text.toString().trim()

        val vehicle =
            binding.etVehicle.text.toString().trim().uppercase()

        val license = binding.etLicense.text.toString().trim()

        val houseNo = binding.etHouseNo.text.toString().trim()
        val street = binding.etStreet.text.toString().trim()
        val pincode = binding.etPincode.text.toString().trim()
        val state = binding.etState.text.toString().trim()
        val city = binding.etCity.text.toString().trim()

        val vehicleType =
            binding.spinnerVehicleType.selectedItem.toString()

        // ================= VALIDATION =================

        if (!phone.matches(Regex("^[6-9][0-9]{9}$"))) {

            binding.etPhone.error = "Enter Valid Phone Number"
            return
        }

        if (!vehicle.matches(
                Regex("^[A-Z]{2}[0-9]{2}[A-Z]{1,2}[0-9]{4}$")
            )
        ) {

            binding.etVehicle.error = "Enter Valid Vehicle Number"
            return
        }

        if (license.length < 8) {

            binding.etLicense.error = "Invalid License ID"
            return
        }

        if (!pincode.matches(Regex("^[0-9]{6}$"))) {

            binding.etPincode.error = "Invalid Pincode"
            return
        }

        if (!city.matches(Regex("^[A-Za-z ]+$"))) {

            binding.etCity.error = "Only alphabets allowed"
            return
        }

        if (!state.matches(Regex("^[A-Za-z ]+$"))) {

            binding.etState.error = "Only alphabets allowed"
            return
        }

        if (vehicleType == "Select Vehicle Type") {

            Toast.makeText(this, "Select Vehicle Type", Toast.LENGTH_SHORT).show()
            return
        }

        // ================= CHECK IMAGES =================

        db.get().addOnSuccessListener { snapshot ->

            val profileImg =
                snapshot.child("profileImage").value?.toString()

            val licenseUrl =
                snapshot.child("licenseUrl").value?.toString()

            val rcUrl =
                snapshot.child("rcUrl").value?.toString()

            val aadharUrl =
                snapshot.child("aadharUrl").value?.toString()

            if (
                profileImg.isNullOrEmpty() ||
                licenseUrl.isNullOrEmpty() ||
                rcUrl.isNullOrEmpty() ||
                aadharUrl.isNullOrEmpty()
            ) {

                Toast.makeText(
                    this,
                    "Upload all documents first",
                    Toast.LENGTH_SHORT
                ).show()

                return@addOnSuccessListener
            }

            // ================= UPDATE =================

            val updates = hashMapOf<String, Any>(

                "phone" to phone,
                "vehicleNumber" to vehicle,
                "licenseId" to license,
                "houseNo" to houseNo,
                "street" to street,
                "pincode" to pincode,
                "state" to state,
                "city" to city,
                "vehicleType" to vehicleType
            )

            binding.btnSave.isEnabled = false

            db.updateChildren(updates)

                .addOnSuccessListener {

                    binding.btnSave.isEnabled = true

                    Toast.makeText(
                        this,
                        "Profile Updated Successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    isEditing = false
                    setEditMode(false)

                    binding.btnEditProfile
                        .setImageResource(R.drawable.edit_off)
                }

                .addOnFailureListener {

                    binding.btnSave.isEnabled = true

                    Toast.makeText(
                        this,
                        it.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    // ================= GALLERY =================

    private val galleryLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                uploadToCloudinary(uri)
            }
        }

    private fun openGallery() {

        galleryLauncher.launch("image/*")
    }

    // ================= CLOUDINARY =================

    private fun uploadToCloudinary(uri: Uri) {

        val inputStream = contentResolver.openInputStream(uri)

        if (inputStream == null) {

            Toast.makeText(this, "File Not Readable", Toast.LENGTH_SHORT).show()
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

        isUploading = true

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {

                runOnUiThread {

                    isUploading = false

                    Toast.makeText(
                        this@MyProfileActivity,
                        "Upload Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {

                val body = response.body?.string()

                if (!response.isSuccessful || body.isNullOrEmpty()) {

                    runOnUiThread {

                        isUploading = false

                        Toast.makeText(
                            this@MyProfileActivity,
                            "Upload Failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    return
                }

                try {

                    val json = JSONObject(body)

                    val url = json.getString("secure_url")

                    runOnUiThread {

                        db.child(imageType).setValue(url)

                        isUploading = false

                        when (imageType) {

                            "profileImage" -> {

                                Glide.with(this@MyProfileActivity)
                                    .load(url)
                                    .circleCrop()
                                    .into(binding.profileImage)
                            }

                            "licenseUrl" ->
                                binding.licenseFileName.text = "License Uploaded"

                            "rcUrl" ->
                                binding.rcFileName.text = "RC Uploaded"

                            "aadharUrl" ->
                                binding.aadharFileName.text = "Aadhar Uploaded"
                        }

                        Toast.makeText(
                            this@MyProfileActivity,
                            "Upload Success",
                            Toast.LENGTH_SHORT
                        ).show()

                        imageType = ""
                    }

                } catch (e: Exception) {

                    runOnUiThread {

                        Toast.makeText(
                            this@MyProfileActivity,
                            "Upload Error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
}