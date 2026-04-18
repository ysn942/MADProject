package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.myapplication.network.WeatherApiService
import com.example.myapplication.network.WeatherResponse
import com.example.myapplication.room.AppDatabase
import com.example.myapplication.room.CoordinatesEntity
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

// ── Couleurs globales ─────────────────────────────────────────────────────────
val LightBlue = Color(0xFFADD8E6)
val DarkBlue  = Color(0xFF0D47A1)

private const val TAG = "MADApp"

// ── MainActivity ──────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {

    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var locationCallback: LocationCallback
    private var latestLocationState = mutableStateOf<Location?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor     = DarkBlue.toArgb()
        window.navigationBarColor = DarkBlue.toArgb()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // W2 : callback mise à jour GPS continue
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                latestLocationState.value = loc
                Log.d(TAG, "New location: ${loc.latitude}, ${loc.longitude}")
                // W6 : sauvegarder chaque nouvelle position dans Room
                lifecycleScope.launch {
                    AppDatabase.getInstance(this@MainActivity).coordinatesDao().insert(
                        CoordinatesEntity(
                            timestamp = System.currentTimeMillis(),
                            latitude  = loc.latitude,
                            longitude = loc.longitude,
                            altitude  = loc.altitude
                        )
                    )
                }
            }
        }

        // Demander permission GPS si nécessaire
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                100
            )
        } else {
            startLocationUpdates()
        }

        setContent {
            MyApplicationTheme {
                val navController: NavHostController = rememberNavController()
                val latestLocation by latestLocationState

                Scaffold { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(LightBlue)
                    ) {
                        NavigationHost(navController, latestLocation)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L  // toutes les 5 secondes
        ).setMinUpdateDistanceMeters(5f).build()

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        Log.d(TAG, "GPS updates started")
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "GPS permission denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume()  { super.onResume() }
    override fun onPause()   {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

// ── Navigation ────────────────────────────────────────────────────────────────
@Composable
fun NavigationHost(navController: NavHostController, latestLocation: Location?) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home")       { PageHome(navController, latestLocation) }
        composable("collection") { PageCollection(navController, latestLocation) }
        composable("settings")   { PageSettings(navController) }
    }
}

// ── Page Home (W1-W2-W6) ─────────────────────────────────────────────────────
@Composable
fun PageHome(navController: NavHostController, latestLocation: Location?) {
    val context  = LocalContext.current
    val activity = context as? MainActivity

    // W6 : état météo
    var weatherText    by remember { mutableStateOf("Loading weather…") }
    var weatherIconUrl by remember { mutableStateOf<String?>(null) }
    var apiKey         by remember { mutableStateOf(prefGetApiKey(context)) }

    // W6 : charger météo dès qu'on a une position ou une clé
    LaunchedEffect(latestLocation, apiKey) {
        val key = apiKey
        if (!key.isNullOrBlank()) {
            val lat = latestLocation?.latitude  ?: 40.3890
            val lon = latestLocation?.longitude ?: -3.6280
            fetchWeather(lat, lon, key) { text, url ->
                weatherText    = text
                weatherIconUrl = url
            }
        } else {
            weatherText = "No API key — set it in Settings"
        }
    }

    // Re-lire la clé quand on revient sur cette page
    LaunchedEffect(Unit) {
        apiKey = prefGetApiKey(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🏠 Home", fontSize = 24.sp, color = DarkBlue, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(16.dp))

        // W2 : afficher les coordonnées GPS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("📡 GPS", color = DarkBlue, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Latitude : ${latestLocation?.latitude?.let  { "%.6f".format(it) } ?: "waiting…"}",
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = DarkBlue
                )
                Text(
                    "Longitude: ${latestLocation?.longitude?.let { "%.6f".format(it) } ?: "waiting…"}",
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = DarkBlue
                )
                Text(
                    "Altitude : ${latestLocation?.altitude?.let  { "%.1f".format(it) } ?: "—"} m",
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = DarkBlue
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // W6 : météo + icône Coil (équivalent Glide)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                weatherIconUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Weather",
                        modifier = Modifier.size(72.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(weatherText, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = DarkBlue)
            }
        }

        Spacer(Modifier.height(24.dp))

        // W3 : bouton carte
        AppButton("🗺️ Map (OpenStreetMap)") {
            val intent = Intent(context, OpenStreetMapsActivity::class.java).apply {
                putExtra("locationBundle", Bundle().apply {
                    putParcelable("location", latestLocation)
                })
            }
            context.startActivity(intent)
        }

        // W5 : bouton collection (liste Room)
        AppButton("📋 Collection (Room DB)") {
            navController.navigate("collection")
        }

        // W4 : bouton réglages (SharedPreferences)
        AppButton("⚙️ Settings") {
            navController.navigate("settings")
        }

        // W7 : bouton Firebase
        Button(
            onClick = {
                context.startActivity(
                    android.content.Intent(context, FirebaseActivity::class.java)
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF360C)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Text("🔥 Firebase", color = Color.White)
        }

        // W2 : bouton démarrer/arrêter GPS manuellement
        var gpsRunning by remember { mutableStateOf(true) }
        OutlinedButton(
            onClick = {
                if (gpsRunning) {
                    activity?.fusedLocationClient?.removeLocationUpdates(activity.locationCallback)
                    Toast.makeText(context, "GPS stopped", Toast.LENGTH_SHORT).show()
                } else {
                    activity?.startLocationUpdates()
                    Toast.makeText(context, "GPS started", Toast.LENGTH_SHORT).show()
                }
                gpsRunning = !gpsRunning
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Text(if (gpsRunning) "⏸ Disable GPS" else "▶ Enable GPS", color = DarkBlue)
        }
    }
}

// ── Page Collection (W5-W6 : liste + CRUD Room) ───────────────────────────────
@Composable
fun PageCollection(navController: NavHostController, latestLocation: Location?) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val sdf     = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    var coords  by remember { mutableStateOf<List<CoordinatesEntity>>(emptyList()) }

    // Charger les données Room
    suspend fun reload() {
        coords = withContext(Dispatchers.IO) {
            AppDatabase.getInstance(context).coordinatesDao().getAll()
        }
    }
    LaunchedEffect(Unit) { reload() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("📋 Collection", fontSize = 22.sp, color = DarkBlue, fontFamily = FontFamily.Monospace)
        Text("${coords.size} record(s) — Room DB", fontSize = 12.sp, color = DarkBlue)
        Spacer(Modifier.height(8.dp))

        // W6 : sauvegarder position actuelle (C - Create)
        AppButton("💾 Save current location") {
            val loc = latestLocation
            if (loc != null) {
                scope.launch {
                    AppDatabase.getInstance(context).coordinatesDao().insert(
                        CoordinatesEntity(
                            timestamp = System.currentTimeMillis(),
                            latitude  = loc.latitude,
                            longitude = loc.longitude,
                            altitude  = loc.altitude
                        )
                    )
                    reload()
                    Toast.makeText(context, "Location saved ✅", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "GPS not available yet", Toast.LENGTH_SHORT).show()
            }
        }

        // Supprimer tout (D - Delete all)
        OutlinedButton(
            onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val db = AppDatabase.getInstance(context)
                        db.coordinatesDao().getAll()
                            .forEach { db.coordinatesDao().deleteByTimestamp(it.timestamp) }
                    }
                    reload()
                    Toast.makeText(context, "All deleted", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) { Text("🗑 Delete all", color = Color.Red) }

        // En-tête tableau (W5 : ListView → LazyColumn Compose)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text("Date/Time",  Modifier.weight(1.5f), color = DarkBlue, fontSize = 10.sp)
            Text("Lat",         Modifier.weight(1f),   color = DarkBlue, fontSize = 10.sp)
            Text("Lon",         Modifier.weight(1f),   color = DarkBlue, fontSize = 10.sp)
            Text("Alt(m)",      Modifier.weight(0.8f), color = DarkBlue, fontSize = 10.sp)
        }
        HorizontalDivider(color = DarkBlue)

        if (coords.isEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("No coordinates saved yet.", color = DarkBlue)
        }

        // W5 : liste scrollable avec suppression individuelle
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(coords, key = { it.timestamp }) { coord ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            sdf.format(Date(coord.timestamp)),
                            Modifier.weight(1.5f), fontSize = 9.sp, color = DarkBlue
                        )
                        Text(
                            "%.5f".format(coord.latitude),
                            Modifier.weight(1f), fontSize = 9.sp, color = DarkBlue
                        )
                        Text(
                            "%.5f".format(coord.longitude),
                            Modifier.weight(1f), fontSize = 9.sp, color = DarkBlue
                        )
                        Text(
                            "%.1f".format(coord.altitude),
                            Modifier.weight(0.8f), fontSize = 9.sp, color = DarkBlue
                        )
                    }
                    // W6 : supprimer un enregistrement (D - Delete)
                    TextButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    AppDatabase.getInstance(context)
                                        .coordinatesDao()
                                        .deleteByTimestamp(coord.timestamp)
                                }
                                reload()
                            }
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("🗑 Delete", color = Color.Red, fontSize = 10.sp)
                    }
                    HorizontalDivider(color = LightBlue)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        AppButton("← Back") { navController.navigateUp() }
    }
}

// ── Page Réglages (W4 : SharedPreferences) ───────────────────────────────────
@Composable
fun PageSettings(navController: NavHostController) {
    val context  = LocalContext.current
    var userId   by remember { mutableStateOf(prefGetUserId(context) ?: "") }
    var apiKey   by remember { mutableStateOf(prefGetApiKey(context) ?: "") }
    var showKey  by remember { mutableStateOf(false) }
    var message  by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⚙️ Settings", fontSize = 22.sp, color = DarkBlue, fontFamily = FontFamily.Monospace)
        Text("SharedPreferences", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(24.dp))

        // W4 : champ User ID
        OutlinedTextField(
            value    = userId,
            onValueChange = { userId = it },
            label    = { Text("User Identifier") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        // W6 : champ API Key OpenWeatherMap (masquée)
        OutlinedTextField(
            value    = apiKey,
            onValueChange = { apiKey = it },
            label    = { Text("OpenWeatherMap API Key") },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "Hide" else "Show", fontSize = 11.sp)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))
        Text(
            "Get a free key at: openweathermap.org/api",
            fontSize = 11.sp, color = Color.Gray
        )

        Spacer(Modifier.height(16.dp))

        // W4 : bouton sauvegarder dans SharedPreferences
        AppButton("💾 Save") {
            prefSaveUserId(context, userId.trim())
            prefSaveApiKey(context, apiKey.trim())
            message = "✅ Settings saved!"
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
        }

        if (message.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color(0xFF2E7D32), fontSize = 14.sp)
        }

        Spacer(Modifier.height(24.dp))

        // W4 : afficher la valeur sauvegardée
        val savedId  = prefGetUserId(context)
        val savedKey = prefGetApiKey(context)
        if (!savedId.isNullOrBlank()) {
            Text("Saved ID: $savedId", fontSize = 12.sp, color = DarkBlue)
        }
        if (!savedKey.isNullOrBlank()) {
            Text("API Key: ${"*".repeat(savedKey.length.coerceAtMost(8))}…", fontSize = 12.sp, color = DarkBlue)
        }

        Spacer(Modifier.height(24.dp))
        AppButton("← Back") { navController.navigateUp() }
    }
}

// ── Composable bouton réutilisable ────────────────────────────────────────────
@Composable
fun AppButton(label: String, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        colors   = ButtonDefaults.buttonColors(containerColor = DarkBlue),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(label, color = Color.White)
    }
}

// ── SharedPreferences helpers (W4) ───────────────────────────────────────────
private fun prefs(ctx: Context) =
    ctx.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

fun prefSaveUserId(ctx: Context, id:  String) = prefs(ctx).edit().putString("USER_ID", id).apply()
fun prefGetUserId (ctx: Context): String?      = prefs(ctx).getString("USER_ID", null)
fun prefSaveApiKey(ctx: Context, key: String) = prefs(ctx).edit().putString("API_KEY", key).apply()
fun prefGetApiKey (ctx: Context): String?      = prefs(ctx).getString("API_KEY", null)

// ── Retrofit — appel API OpenWeatherMap (W6) ──────────────────────────────────
fun fetchWeather(lat: Double, lon: Double, apiKey: String, onResult: (String, String?) -> Unit) {
    Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApiService::class.java)
        .getWeather(lat, lon, 1, apiKey)
        .enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                val item = response.body()?.list?.firstOrNull()
                if (item != null) {
                    val tempC   = item.main.temp - 273.15
                    val text    = "📍 ${item.name}\n" +
                                  "🌡 ${"%.1f".format(tempC)}°C  " +
                                  "💧 ${item.main.humidity}%\n" +
                                  "☁️ ${item.weather.firstOrNull()?.description ?: ""}"
                    val iconUrl = item.weather.firstOrNull()?.icon?.let {
                        "https://openweathermap.org/img/wn/${it}@2x.png"
                    }
                    onResult(text, iconUrl)
                } else {
                    onResult("Empty response — check your API key\n(code ${response.code()})", null)
                }
            }
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e(TAG, "Retrofit error: ${t.message}")
                onResult("Network error: ${t.message}", null)
            }
        })
}
