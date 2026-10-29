
plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.collegelostandfound"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.collegelostandfound"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // Existing Android libraries
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Firebase BoM
    implementation(
        platform("com.google.firebase:firebase-bom:34.4.0")
    )

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // Firebase Realtime Database
    implementation("com.google.firebase:firebase-database")

    // Supabase Kotlin SDK
    implementation(
        platform("io.github.jan-tennert.supabase:bom:3.5.0")
    )

    // Supabase Storage for item photos
    implementation("io.github.jan-tennert.supabase:storage-kt")

    // HTTP client for Supabase
    implementation("io.ktor:ktor-client-okhttp:3.0.3")

    // Existing testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
}
