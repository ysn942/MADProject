package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * W3 : Affiche OpenStreetMap avec :
 *  - Position GPS actuelle
 *  - Route prédéfinie (gymkhana campus)
 *  - Points sauvegardés depuis Room DB (W6)
 *  - Tracé manuel par touch
 */
class OpenStreetMapsActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val userPoints = mutableListOf<GeoPoint>()
    private lateinit var userPolyline: Polyline
    private val TAG = "MapActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100
            )
        }

        setContentView(R.layout.activity_open_street_maps)

        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))

        val bundle = intent.getBundleExtra("locationBundle")
        @Suppress("DEPRECATION")
        val location: Location? = bundle?.getParcelable("location")
        val startPoint = if (location != null) {
            GeoPoint(location.latitude, location.longitude)
        } else {
            GeoPoint(40.3890, -3.6280) // Campus Sur UPM
        }

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.controller.setZoom(18.0)
        map.controller.setCenter(startPoint)
        map.setMultiTouchControls(true)

        // Marqueur position actuelle
        addMarker(startPoint, "Ma position actuelle", android.R.drawable.ic_secure)

        // Polyline pour tracé manuel (touch)
        userPolyline = Polyline().apply { width = 8f }
        map.overlays.add(userPolyline)

        // Touch : ajouter un point à la route manuelle
        map.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val geo = map.projection.fromPixels(event.x.toInt(), event.y.toInt())
                val pt  = GeoPoint(geo.latitude, geo.longitude)
                userPoints.add(pt)
                userPolyline.setPoints(userPoints)
                addMarker(pt, "Point ajouté", android.R.drawable.ic_menu_add)
                map.invalidate()
            }
            false // false = ne pas bloquer le zoom/scroll natif
        }

        // Route gymkhana pré-définie (W3)
        val gymCoords = listOf(
            GeoPoint(40.38779608214728, -3.627687914352839),
            GeoPoint(40.38788595319803, -3.627048250272035),
            GeoPoint(40.38873152245420, -3.628643539758645),
            GeoPoint(40.38926842612264, -3.630067893975619),
            GeoPoint(40.38956358584258, -3.629046081389352),
            GeoPoint(40.38992125672989, -3.628136649776971),
            GeoPoint(40.39037466191718, -3.627025676359844),
            GeoPoint(40.38985588480300, -3.626782180787362)
        )
        val gymNames = listOf(
            "Tennis", "Futsal", "Mode/Design",
            "Topographie", "Télécoms", "ETSISI", "Bibliothèque", "CITSEM"
        )
        addRouteWithMarkers(gymCoords, gymNames)

        // Charger les points Room DB et les afficher sur la carte (W6)
        loadRoomMarkers()
    }

    private fun loadRoomMarkers() {
        lifecycleScope.launch(Dispatchers.IO) {
            val saved = AppDatabase.getInstance(this@OpenStreetMapsActivity)
                .coordinatesDao().getAll()
            withContext(Dispatchers.Main) {
                saved.forEach { coord ->
                    addMarker(
                        GeoPoint(coord.latitude, coord.longitude),
                        "GPS sauvegardé\nLat: ${"%.4f".format(coord.latitude)}, Lon: ${"%.4f".format(coord.longitude)}",
                        android.R.drawable.ic_menu_mylocation
                    )
                }
                map.invalidate()
                Log.d(TAG, "${saved.size} points Room affichés sur la carte")
            }
        }
    }

    private fun addMarker(point: GeoPoint, title: String, iconRes: Int) {
        val marker = Marker(map).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(this@OpenStreetMapsActivity, iconRes)
            this.title = title
        }
        map.overlays.add(marker)
    }

    private fun addRouteWithMarkers(coords: List<GeoPoint>, names: List<String>) {
        val polyline = Polyline().apply { setPoints(coords) }
        map.overlays.add(polyline)
        coords.indices.forEach { i ->
            addMarker(coords[i], names[i], android.R.drawable.ic_menu_compass)
        }
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause()  { super.onPause();  map.onPause()  }
}
