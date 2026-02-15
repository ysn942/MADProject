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
import android.content.pm.PackageManager
import android.location.Location
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

        // Couleur status bar et navigation bar
        window.statusBarColor = Color(0xFF0D47A1).toArgb()
        window.navigationBarColor = Color(0xFF0D47A1).toArgb()

        setContent {
            MyApplicationTheme {
                val navController: NavHostController = rememberNavController()

                Scaffold { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(Color(0xFFADD8E6)) // fond bleu clair
                    ) {
                        NavigationHost(navController = navController)
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
fun NavigationHost(navController: NavHostController) { // def des boutons et des directions vers lequelles elles vont
    NavHost(navController = navController, startDestination = "page1") {
        composable("page1") { Page1(navController = navController) }
        composable("page2") { ThreeButtonsPage(currentPage = 2, navController = navController) }
        composable("page3") { ThreeButtonsPage(currentPage = 3, navController = navController) }
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
fun ThreeButtonsPage(currentPage: Int, navController: NavHostController) { // Def des pages avec pr chaque page un bouton pour l'instant
    val lightBlue = Color(0xFFADD8E6)
    val darkBlue = Color(0xFF0D47A1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val pages = listOf(1, 2, 3)
        pages.filter { it != currentPage }.forEach { page ->
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