package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class OpenStreetMapsActivity : ComponentActivity() {

    private val userRoutePoints = mutableListOf<GeoPoint>()
    private lateinit var userRoutePolyline: Polyline
    private lateinit var map: MapView
    private val TAG = "OpenStreetMapsActivity"
    private val LOCATION_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ------------------------
        // Permissions runtime
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }

        // ------------------------
        // Layout simple avec MapView
        setContentView(R.layout.activity_open_street_maps)

        // ------------------------
        // Config osmdroid
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))

        // ------------------------
        // Récupérer location depuis MainActivity
        val bundle = intent.getBundleExtra("locationBundle")
        val location: Location? = bundle?.getParcelable("location")

        // Coordonnées de départ (emulateur ou par défaut)
        val startPoint = location?.let {
            Log.d(TAG, "Location reçue: lat=${it.latitude}, lon=${it.longitude}")
            GeoPoint(it.latitude, it.longitude)
        } ?: GeoPoint(40.3890, -3.6280) // Campus Sur UPM

        // ------------------------
        // MapView
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.controller.setZoom(18.0)
        map.controller.setCenter(startPoint)

        // ------------------------
        // Marker position actuelle
        val currentMarker = Marker(map).apply {
            position = startPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(this@OpenStreetMapsActivity, android.R.drawable.ic_delete) as BitmapDrawable
            title = "My current location"
        }
        map.overlays.add(currentMarker)

        // ------------------------
        // Initialise la Polyline pour le tracé utilisateur
        userRoutePolyline = Polyline().apply {
            width = 6f
            color = 0xFF0D47A1.toInt() // Bleu foncé
        }
        map.overlays.add(userRoutePolyline)

        // ------------------------
        // Touch listener pour ajouter des points
        map.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val iGeo = map.projection.fromPixels(event.x.toInt(), event.y.toInt())
                val geoPoint = GeoPoint(iGeo.latitude, iGeo.longitude) // Conversion IGeoPoint -> GeoPoint
                addPointToRoute(geoPoint)
            }
            true
        }

        // ------------------------
        // Points et route pré-définis (gymkhana)
        val gymkhanaCoords = listOf(
            GeoPoint(40.38779608214728, -3.627687914352839),
            GeoPoint(40.38788595319803, -3.627048250272035),
            GeoPoint(40.3887315224542, -3.628643539758645),
            GeoPoint(40.38926842612264, -3.630067893975619)
        )
        val gymkhanaNames = listOf(
            "Tennis", "Futsal outdoors", "Fashion and design school", "Topography school"
        )
        addRouteMarkers(map, gymkhanaCoords, gymkhanaNames, this)
    }

    // ------------------------
    // Fonction pour ajouter une route + markers
    private fun addRouteMarkers(map: MapView, coords: List<GeoPoint>, names: List<String>, context: Context) {
        val polyline = Polyline().apply { setPoints(coords) }

        coords.indices.forEach { i ->
            val marker = Marker(map).apply {
                position = coords[i]
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_compass) as BitmapDrawable
                title = names[i]
            }
            map.overlays.add(marker)
        }

        map.overlays.add(polyline)
    }

    // ------------------------
    // Ajouter un point tracé par l'utilisateur
    private fun addPointToRoute(point: GeoPoint) {
        userRoutePoints.add(point)
        userRoutePolyline.setPoints(userRoutePoints)

        val marker = Marker(map).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(this@OpenStreetMapsActivity, android.R.drawable.ic_menu_compass) as BitmapDrawable
        }
        map.overlays.add(marker)
        map.invalidate()
    }

    // ------------------------
    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}