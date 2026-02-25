package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme
import android.util.Log
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() { // paramètres de l'appli avec la création et personnalisation des thèmes, des autorisations, connexions vers google etc..
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }
        // Couleur status bar et navigation bar
        window.statusBarColor = Color(0xFF0D47A1).toArgb()
        window.navigationBarColor = Color(0xFF0D47A1).toArgb()

        setContent {
            MyApplicationTheme {
                val navController: NavHostController = rememberNavController()
                var latestLocation by remember { mutableStateOf<Location?>(null) }
                val context = LocalContext.current
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            latestLocation = location
                        }
                    }
                }

                Scaffold { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(Color(0xFFADD8E6)) // fond bleu clair
                    ) {
                        NavigationHost(navController = navController, latestLocation = latestLocation)
                    }
                }
            }
        }
        Log.v("GPS_APP", "Verbose log")
        Log.d("GPS_APP", "Debug log")
        Log.i("GPS_APP", "Info log")
        Log.w("GPS_APP", "Warning log")
        Log.e("GPS_APP", "Error log")

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Permission check
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        } else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val lat = location.latitude
                        val lon = location.longitude

                        Log.i("GPS_APP", "Latitude = $lat")
                        Log.i("GPS_APP", "Longitude = $lon")
                    } else {
                        Log.w("GPS_APP", "Location is null")
                    }
                }
        }
    }
}

// --------------------
// Navigation et Pages
// --------------------

@Composable
fun NavigationHost(navController: NavHostController, latestLocation: Location?) { // def des boutons et des directions vers lequelles elles vont
    NavHost(navController = navController, startDestination = "page1") {
        composable("page1") { Page1(navController = navController) }
        composable("page2") { Page2(navController = navController, latestLocation) }
        composable("page3") { Page3(navController = navController, latestLocation) }
    }
}

@Composable
fun Page1(navController: NavHostController) { // def de la page 1 avec les données à afficher
    val lightBlue = Color(0xFFADD8E6)
    val darkBlue = Color(0xFF0D47A1)

    // State pour la latitude et longitude
    var latitude by remember { mutableStateOf("loading...") }
    var longitude by remember { mutableStateOf("loading...") }

    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Permission check
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    latitude = location.latitude.toString()
                    longitude = location.longitude.toString()
                    android.util.Log.i("GPS_APP", "Lat=$latitude, Lon=$longitude")
                } else {
                    latitude = "null"
                    longitude = "null"
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                context as ComponentActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Affichage de la latitude/longitude
        Text(text = "Latitude: $latitude", fontFamily = FontFamily.Monospace, color = darkBlue)
        Text(text = "Longitude: $longitude", fontFamily = FontFamily.Monospace, color = darkBlue)

        Spacer(modifier = Modifier.height(24.dp))

        // Boutons pour naviguer
        val pages = listOf(2, 3) // Page1 ne doit pas montrer elle-même
        pages.forEach { page ->
            Button(
                onClick = { navController.navigate("page$page") },
                colors = ButtonDefaults.buttonColors(containerColor = lightBlue),
                border = BorderStroke(2.dp, darkBlue),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Page $page",
                    color = darkBlue,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun Page2(navController: NavHostController, latestLocation: Location?) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity // sécurise le cast
    val lightBlue = Color(0xFFADD8E6)
    val darkBlue = Color(0xFF0D47A1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Page 2: OpenStreetMap",
            color = darkBlue,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (latestLocation != null) {
                    if (activity != null) {
                        val intent = Intent(activity, OpenStreetMapsActivity::class.java).apply {
                            putExtra("locationBundle", Bundle().apply {
                                putParcelable("location", latestLocation)
                            })
                        }
                        activity.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Cannot start map: Activity not available", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Location not available", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = lightBlue),
            border = BorderStroke(2.dp, darkBlue),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(text = "Launch Map", color = darkBlue)
        }
    }
}

@Composable
fun Page3(navController: NavHostController, latestLocation: Location?) {
    val lightBlue = Color(0xFFADD8E6)
    val darkBlue = Color(0xFF0D47A1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Page 3",
            fontFamily = FontFamily.Monospace,
            color = darkBlue
        )

        Text(
            text = "Latitude: ${latestLocation?.latitude ?: "loading..."}",
            fontFamily = FontFamily.Monospace,
            color = darkBlue
        )
        Text(
            text = "Longitude: ${latestLocation?.longitude ?: "loading..."}",
            fontFamily = FontFamily.Monospace,
            color = darkBlue
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Boutons vers les autres pages
        listOf(1,2).forEach { page ->
            Button(
                onClick = { navController.navigate("page$page") },
                colors = ButtonDefaults.buttonColors(containerColor = lightBlue),
                border = BorderStroke(2.dp, darkBlue),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("Page $page", color = darkBlue, fontFamily = FontFamily.Monospace)
            }
        }
    }
}