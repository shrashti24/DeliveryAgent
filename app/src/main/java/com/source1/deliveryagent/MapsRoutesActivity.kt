package com.source1.deliveryagent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*

import com.source1.deliveryagent.model.OrderModel

class MapsRoutesActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private var restaurantLatLng: LatLng? = null
    private var customerLatLng: LatLng? = null

    private lateinit var txtPickup: TextView
    private lateinit var txtDrop: TextView
    private lateinit var txtDistance: TextView
    private lateinit var txtEstimatedTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps_routes)

        database = FirebaseDatabase.getInstance().reference

        // Views
        txtPickup = findViewById(R.id.txtPickup)
        txtDrop = findViewById(R.id.txtDrop)
        txtDistance = findViewById(R.id.txtDistance)
        txtEstimatedTime = findViewById(R.id.txtEstimatedTime)

        // Back Button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Open Google Maps
        findViewById<TextView>(R.id.btnOpenMaps).setOnClickListener {
            openGoogleMaps()
        }

        // Share Location
        findViewById<TextView>(R.id.btnShareLiveLocation).setOnClickListener {
            shareLiveLocation()
        }

        // Map Fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_route) as SupportMapFragment

        mapFragment.getMapAsync(this)

        // Get Order ID
        val orderId = intent.getStringExtra("orderId")

        if (orderId != null) {

            database.child("Orders")
                .child(orderId)
                .addListenerForSingleValueEvent(object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {

                        val order = snapshot.getValue(OrderModel::class.java)

                        if (order != null) {

                            // Coordinates
                            val rLat = order.restaurantLat ?: 0.0
                            val rLng = order.restaurantLng ?: 0.0

                            val cLat = order.customerLat ?: 0.0
                            val cLng = order.customerLng ?: 0.0

                            restaurantLatLng = LatLng(rLat, rLng)
                            customerLatLng = LatLng(cLat, cLng)

                            // Set UI Data
                            txtPickup.text =
                                "Pickup: ${order.restaurantName ?: "Restaurant"}"

                            txtDrop.text =
                                "Drop: ${order.address ?: "No Address"}"

                            txtDistance.text =
                                "Distance: ${order.distance ?: "Not Available"}"

                            txtEstimatedTime.text =
                                "Estimated time: ${order.estimatedTime ?: "Not Available"}"

                            // Update Map
                            if (::mMap.isInitialized) {
                                updateMap()
                            }

                        } else {
                            Toast.makeText(
                                this@MapsRoutesActivity,
                                "Order data not found",
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
    }

    // MAP READY
    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap

        enableMyLocation()

        updateMap()
    }

    // UPDATE MAP
    private fun updateMap() {

        mMap.clear()

        // Restaurant Marker
        restaurantLatLng?.let {

            mMap.addMarker(
                MarkerOptions()
                    .position(it)
                    .title("Restaurant")
            )

            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(it, 14f)
            )
        }

        // Customer Marker
        customerLatLng?.let {

            mMap.addMarker(
                MarkerOptions()
                    .position(it)
                    .title("Customer")
            )
        }
    }

    // OPEN GOOGLE MAPS
    private fun openGoogleMaps() {

        if (customerLatLng == null) {

            Toast.makeText(
                this,
                "Customer location not available",
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

    // SHARE LOCATION
    private fun shareLiveLocation() {

        if (customerLatLng == null) {

            Toast.makeText(
                this,
                "Location not available",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val uri =
            "http://maps.google.com/maps?q=loc:${customerLatLng!!.latitude},${customerLatLng!!.longitude}"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {

            type = "text/plain"

            putExtra(
                Intent.EXTRA_TEXT,
                uri
            )
        }

        startActivity(
            Intent.createChooser(
                shareIntent,
                "Share via"
            )
        )
    }

    // ENABLE LOCATION
    private fun enableMyLocation() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            mMap.isMyLocationEnabled = true

            return
        }

        // Request Permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // PERMISSION RESULT
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

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {

            if (
                grantResults.isNotEmpty() &&
                (
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                                ||
                                grantResults[1] == PackageManager.PERMISSION_GRANTED
                        )
            ) {

                enableMyLocation()

            } else {

                Toast.makeText(
                    this,
                    "Permission denied to access location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}