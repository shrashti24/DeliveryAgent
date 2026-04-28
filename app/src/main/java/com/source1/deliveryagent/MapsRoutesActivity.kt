package com.source1.deliveryagent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsRoutesActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var database: DatabaseReference
    private var restaurantLatLng: LatLng? = null
    private var customerLatLng: LatLng? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps_routes)
        database = FirebaseDatabase.getInstance().reference

        val orderId = intent.getStringExtra("orderId")

        if (orderId != null) {
            database.child("Orders").child(orderId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {

                        val rLat = snapshot.child("restaurantLat").getValue(Double::class.java) ?: 0.0
                        val rLng = snapshot.child("restaurantLng").getValue(Double::class.java) ?: 0.0

                        val cLat = snapshot.child("customerLat").getValue(Double::class.java) ?: 0.0
                        val cLng = snapshot.child("customerLng").getValue(Double::class.java) ?: 0.0

                        restaurantLatLng = LatLng(rLat, rLng)
                        customerLatLng = LatLng(cLat, cLng)
                        if (::mMap.isInitialized) {
                            updateMap()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.btnOpenMaps).setOnClickListener {
            openGoogleMaps()
        }

        findViewById<TextView>(R.id.btnShareLiveLocation).setOnClickListener {
            shareLiveLocation()
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_route) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    private fun updateMap() {

        mMap.clear()

        restaurantLatLng?.let {
            mMap.addMarker(MarkerOptions().position(it).title("Restaurant"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 14f))
        }

        customerLatLng?.let {
            mMap.addMarker(MarkerOptions().position(it).title("Customer"))
        }
    }
    private fun openGoogleMaps() {

        if (customerLatLng == null) return

        val uri = Uri.parse("google.navigation:q=${customerLatLng!!.latitude},${customerLatLng!!.longitude}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        startActivity(intent)
    }

    private fun shareLiveLocation() {

        if (customerLatLng == null) return

        val uri = "http://maps.google.com/maps?q=loc:${customerLatLng!!.latitude},${customerLatLng!!.longitude}"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, uri)
        }

        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()
        updateMap()
        restaurantLatLng?.let {
            mMap.addMarker(MarkerOptions().position(it).title("Restaurant"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 14f))
        }

        customerLatLng?.let {
            mMap.addMarker(MarkerOptions().position(it).title("Customer"))
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            return
        }

        // If permission is not granted, request it
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Permission denied to access location", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
