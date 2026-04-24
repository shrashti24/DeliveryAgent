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

class MapsRoutesActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val LUCKNOW_LAT_LNG = LatLng(26.8467, 80.9462)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps_routes)

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

    private fun openGoogleMaps() {
        val latitude = LUCKNOW_LAT_LNG.latitude
        val longitude = LUCKNOW_LAT_LNG.longitude
        val label = "Hazratganj, Lucknow"
        val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            startActivity(fallbackIntent)
        }
    }

    private fun shareLiveLocation() {
        // In a real app, you'd get the actual current LatLng. 
        // Here we use the Lucknow location as the "Live" location for the agent.
        val latitude = LUCKNOW_LAT_LNG.latitude
        val longitude = LUCKNOW_LAT_LNG.longitude
        val uri = "http://maps.google.com/maps?q=loc:$latitude,$longitude (Delivery Agent Live Location)"
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Live Location")
            putExtra(Intent.EXTRA_TEXT, "I am currently out for delivery. Track my live location here: $uri")
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()

        // Updated to Lucknow coordinates
        mMap.addMarker(MarkerOptions().position(LUCKNOW_LAT_LNG).title("Marker in Lucknow"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LUCKNOW_LAT_LNG, 12f))
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
