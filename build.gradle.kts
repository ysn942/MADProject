plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    // W7 Firebase — décommenter APRÈS avoir mis google-services.json dans app/
    id("com.google.gms.google-services") version "4.4.2" apply false
}
