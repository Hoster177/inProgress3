plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt") version "1.9.25" // For Room annotation processing
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "ru.hoster.inprogress"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.hoster.inprogress"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel & LiveData/StateFlow (Lifecycle)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0") // For collectAsStateWithLifecycle

    // Room (SQLite ORM)
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1") // Kotlin Extensions and Coroutines support for Room

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.13.0")) // Use the latest BOM
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx") // For Firestore (or firebase-database-ktx for Realtime DB)firebase-firestore-ktx
    implementation("com.google.firebase:firebase-storage-ktx") // For avatar storage

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")


    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.compose.material:material-icons-core:1.6.7") // Or latest version
    implementation("androidx.compose.material:material-icons-extended:1.6.7") // Or latest version


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.google.dagger:hilt-android:2.51.1") // Check for latest version
    kapt("com.google.dagger:hilt-compiler:2.51.1") // For kapt
    // OR for KSP (if you set it up):
    // ksp("com.google.dagger:hilt-compiler:2.51.1")


    // For ViewModel injection
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0") // Check for latest version
}

    // Allow references to generated code
    kapt { // if using kapt
        correctErrorTypes = true
}