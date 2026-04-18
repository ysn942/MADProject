package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()
        window.statusBarColor     = DarkBlue.toArgb()
        window.navigationBarColor = DarkBlue.toArgb()

        setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().background(LightBlue)) {
                    // Si déjà connecté → écran principal, sinon → login
                    if (auth.currentUser != null) {
                        FirebaseMainScreen(auth = auth, onLogout = {
                            auth.signOut()
                            recreate()
                        }, onBack = { finish() })
                    } else {
                        FirebaseLoginScreen(auth = auth, onLoginSuccess = { recreate() }, onBack = { finish() })
                    }
                }
            }
        }
    }
}

// ── Écran de connexion ────────────────────────────────────────────────────────
@Composable
fun FirebaseLoginScreen(auth: FirebaseAuth, onLoginSuccess: () -> Unit, onBack: () -> Unit) {
    val context  = LocalContext.current
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status   by remember { mutableStateOf("") }
    var loading  by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔥 Firebase", fontSize = 22.sp, color = DarkBlue, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        if (status.isNotEmpty()) {
            Text(status, color = Color.Red, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Bouton Sign In
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        status = "Please fill in all fields"
                        return@Button
                    }
                    loading = true
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            loading = false
                            Toast.makeText(context, "Signed in!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        }
                        .addOnFailureListener { e ->
                            loading = false
                            status = e.message ?: "Sign in failed"
                        }
                },
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                modifier = Modifier.weight(1f)
            ) { Text(if (loading) "Loading…" else "Sign In", color = Color.White, fontSize = 12.sp) }

            // Bouton Create Account
            OutlinedButton(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        status = "Please fill in all fields"
                        return@OutlinedButton
                    }
                    loading = true
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            loading = false
                            Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        }
                        .addOnFailureListener { e ->
                            loading = false
                            status = e.message ?: "Registration failed"
                        }
                },
                enabled = !loading,
                modifier = Modifier.weight(1f)
            ) { Text("Create Account", fontSize = 12.sp) }
        }

        Spacer(Modifier.height(24.dp))
        AppButton("← Back") { onBack() }
    }
}

// ── Écran principal après connexion ──────────────────────────────────────────
@Composable
fun FirebaseMainScreen(auth: FirebaseAuth, onLogout: () -> Unit, onBack: () -> Unit) {
    val context   = LocalContext.current
    val user      = auth.currentUser
    var newReport by remember { mutableStateOf("") }
    var reports   by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Écouter les rapports en temps réel depuis Firebase Realtime DB
    val dbRef = remember {
        FirebaseDatabase.getInstance().reference.child("hotspots")
    }

    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Map<String, Any>>()
                snapshot.children.forEach { child ->
                    val item = child.value
                    if (item is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        list.add(item as Map<String, Any>)
                    }
                }
                reports = list.reversed()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "DB error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        dbRef.addValueEventListener(listener)
        onDispose { dbRef.removeEventListener(listener) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("🔥 Firebase", fontSize = 22.sp, color = DarkBlue, fontFamily = FontFamily.Monospace)
        Text("Signed in as: ${user?.email ?: "unknown"}", fontSize = 12.sp, color = DarkBlue)
        Spacer(Modifier.height(16.dp))

        // Champ + bouton pour ajouter un rapport (hotspot)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newReport, onValueChange = { newReport = it },
                label = { Text("Add a report…") }, singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val text = newReport.trim()
                    if (text.isNotEmpty() && user != null) {
                        val report = mapOf(
                            "userId"    to user.uid,
                            "email"     to (user.email ?: ""),
                            "report"    to text,
                            "timestamp" to System.currentTimeMillis()
                        )
                        FirebaseDatabase.getInstance().reference
                            .child("hotspots").push().setValue(report)
                            .addOnSuccessListener {
                                newReport = ""
                                Toast.makeText(context, "Report saved! ✅", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
            ) { Text("Send", color = Color.White) }
        }

        Spacer(Modifier.height(12.dp))
        Text("Reports (realtime):", fontSize = 13.sp, color = DarkBlue, fontFamily = FontFamily.Monospace)
        HorizontalDivider(color = DarkBlue)

        if (reports.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("No reports yet.", color = DarkBlue, fontSize = 13.sp)
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(reports) { r ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text("📝 ${r["report"]}", color = DarkBlue, fontFamily = FontFamily.Monospace)
                        Text("👤 ${r["email"]}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Logout", color = DarkBlue)
        }
        Spacer(Modifier.height(4.dp))
        AppButton("← Back") { onBack() }
    }
}
