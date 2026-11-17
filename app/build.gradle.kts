plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
// Optional ASCII build path for Windows JVM test workers in a Cyrillic checkout.
providers.environmentVariable("OHMLETTE_BUILD_DIR").orNull?.let { layout.buildDirectory.set(file(it)) }
android {
    namespace = "ua.chyzhevskyi.ohmlette"
    compileSdk = 37
    defaultConfig {
        applicationId = "ua.chyzhevskyi.ohmlette"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
