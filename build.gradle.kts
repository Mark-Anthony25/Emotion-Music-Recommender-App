plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false

}

buildscript {
    dependencies {
        // Add the Google Services Gradle plugin inside the buildscript dependencies
        classpath("com.google.gms:google-services:4.4.2")
        classpath ("androidx.navigation:navigation-safe-args-gradle-plugin:2.7.1")
        //classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.8.1"
        classpath ("com.google.firebase:perf-plugin:1.4.2")
    }
}
