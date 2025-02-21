plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // Correct way to apply Google Services plugin
    id("androidx.navigation.safeargs") // Apply Safe Args plugin
    // Add the Performance Monitoring Gradle plugin
    id("com.google.firebase.firebase-perf")

}

android {
    namespace = "com.ebmr.myapplication1"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ebmr.myapplication1"
        minSdk = 23
        targetSdk = 34
        versionCode = 5
        versionName = "1.5"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        mlModelBinding = true

    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)
    implementation("com.google.mlkit:face-detection:16.1.7")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.7.1")
    implementation ("androidx.navigation:navigation-ui-ktx:2.7.1")
    implementation(libs.vision.common)
    implementation(libs.firebase.database)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.1")
    implementation ("pl.droidsonroids.gif:android-gif-drawable:1.2.+")
    implementation ("com.github.bumptech.glide:glide:4.15.1")

    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")

    // Add Firestore library dependency
    //implementation(libs.firebase.firestore.v2510)
    implementation("com.google.firebase:firebase-firestore:25.1.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Import the BoM for the Firebase platform
    //implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    // Add Coroutine dependencies
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")

    // Add the dependency for the Performance Monitoring library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    //implementation("com.google.firebase:firebase-perf")
    implementation ("com.google.firebase:firebase-perf:21.0.1")

}
