plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    // W7 Firebase — décommenter APRÈS étape 1 et 2 ci-dessus :
    id("com.google.gms.google-services")
}

android {
    namespace   = "com.example.myapplication"
    compileSdk  = 35

    defaultConfig {
        applicationId         = "com.example.myapplication"
        minSdk                = 24
        targetSdk             = 35
        versionCode           = 1
        versionName           = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core Kotlin + Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM (gère les versions Compose automatiquement)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Navigation Compose (W2 - navigation entre pages)
    implementation(libs.androidx.navigation.compose)

    // AppCompat + Material + ConstraintLayout (nécessaire pour OpenStreetMapsActivity en XML)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.12.0")

    // OpenStreetMap (W3 - carte)
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // GPS fusionné Google (W2 - localisation)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Room (W6 - base de données locale)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Retrofit + Gson (W6 - API REST météo)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Coil (W6 - images depuis URL, équivalent Glide pour Compose)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Coroutines (W6 - opérations asynchrones Room/Retrofit)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ── W7 Firebase ─────────────────────────────────────────────────────────
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    // ────────────────────────────────────────────────────────────────────────

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
