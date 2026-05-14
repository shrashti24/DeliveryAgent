package com.source1.deliveryagent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import com.source1.deliveryagent.model.OrderModel

class MapsRoutesActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference

    private lateinit var txtPickup: TextView
    private lateinit var txtDrop: TextView
    private lateinit var txtDistance: TextView
    private lateinit var txtEstimatedTime: TextView

    private var restaurantLatLng: LatLng? = null
    private var customerLatLng: LatLng? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps_routes)

        database = FirebaseDatabase.getInstance().reference

        txtPickup = findViewById(R.id.txtPickup)
        txtDrop = findViewById(R.id.txtDrop)
        txtDistance = findViewById(R.id.txtDistance)
        txtEstimatedTime = findViewById(R.id.txtEstimatedTime)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.btnOpenMaps).setOnClickListener {
            openGoogleMaps()
        }

        findViewById<TextView>(R.id.btnShareLiveLocation).setOnClickListener {
            shareLocation()
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_route) as SupportMapFragment

        mapFragment.getMapAsync(this)

        loadOrderData()
    }

    private fun loadOrderData() {

        val orderId = intent.getStringExtra("orderId")

        if (orderId.isNullOrEmpty()) {
            Toast.makeText(this, "Order ID missing", Toast.LENGTH_SHORT).show()
            return
        }

        database.child("Orders")
            .child(orderId)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val order = snapshot.getValue(OrderModel::class.java)

                    if (order != null) {

                        // Restaurant Location
                        val rLat = order.restaurantLat ?: 0.0
                        val rLng = order.restaurantLng ?: 0.0

                        // Customer Location
                        val cLat = order.customerLat ?: 0.0
                        val cLng = order.customerLng ?: 0.0

                        restaurantLatLng = LatLng(rLat, rLng)
                        customerLatLng = LatLng(cLat, cLng)

                        // Pickup
                        txtPickup.text =
                            "Pickup: ${order.restaurantName ?: "SpicyBite Restaurant"}"

                        // Drop
                        txtDrop.text =
                            "Drop: ${order.address ?: "Customer Address"}"

                        // Calculate Distance
                        calculateDistanceAndTime()

                        if (::mMap.isInitialized) {
                            updateMap()
                        }

                    } else {

                        Toast.makeText(
                            this@MapsRoutesActivity,
                            "Order not found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                    Toast.makeText(
                        this@MapsRoutesActivity,
                        error.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun calculateDistanceAndTime() {

        if (restaurantLatLng == null || customerLatLng == null) return

        val result = FloatArray(1)

        Location.distanceBetween(
            restaurantLatLng!!.latitude,
            restaurantLatLng!!.longitude,
            customerLatLng!!.latitude,
            customerLatLng!!.longitude,
            result
        )

        val distanceInKm = result[0] / 1000

        txtDistance.text =
            "Distance: %.2f km".format(distanceInKm)

        // Approximate ETA
        val estimatedMinutes = (distanceInKm * 4).toInt()

        txtEstimatedTime.text =
            "Estimated time: $estimatedMinutes mins"
    }

    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap

        enableMyLocation()

        updateMap()
    }

    private fun updateMap() {

        mMap.clear()

        if (restaurantLatLng != null) {

            mMap.addMarker(
                MarkerOptions()
                    .position(restaurantLatLng!!)
                    .title("Restaurant")
            )
        }

        if (customerLatLng != null) {

            mMap.addMarker(
                MarkerOptions()
                    .position(customerLatLng!!)
                    .title("Customer")
            )
        }

        // Show both markers in screen
        if (restaurantLatLng != null && customerLatLng != null) {

            val builder = LatLngBounds.Builder()

            builder.include(restaurantLatLng!!)
            builder.include(customerLatLng!!)

            val bounds = builder.build()

            mMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, 150)
            )
        }
    }

    private fun openGoogleMaps() {

        if (customerLatLng == null) {

            Toast.makeText(
                this,
                "Customer location unavailable",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val uri = Uri.parse(
            "google.navigation:q=${customerLatLng!!.latitude},${customerLatLng!!.longitude}"
        )

        val intent = Intent(Intent.ACTION_VIEW, uri)

        intent.setPackage("com.google.android.apps.maps")

        startActivity(intent)
    }

    private fun shareLocation() {

        if (customerLatLng == null) return

        val url =
            "https://maps.google.com/?q=${customerLatLng!!.latitude},${customerLatLng!!.longitude}"

        val intent = Intent(Intent.ACTION_SEND)

        intent.type = "text/plain"

        intent.putExtra(Intent.EXTRA_TEXT, url)

        startActivity(Intent.createChooser(intent, "Share Location"))
    }

    private fun enableMyLocation() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            mMap.isMyLocationEnabled = true

        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {

            enableMyLocation()
        }
    }
}