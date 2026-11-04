import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
val appVersion = Properties().apply { rootProject.file("version.properties").inputStream().use { load(it) } }
val releaseStore = providers.environmentVariable("WAYPOINT_KEYSTORE").orNull

android {
    namespace = "com.waypoint.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.waypoint.app"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersion.getProperty("VERSION_CODE").toInt()
        versionName = appVersion.getProperty("VERSION_NAME")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    signingConfigs {
        if (releaseStore != null) create("distribution") {
            storeFile = file(releaseStore)
            storePassword = providers.environmentVariable("WAYPOINT_STORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("WAYPOINT_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("WAYPOINT_KEY_PASSWORD").get()
        }
    }
    buildTypes {
        create("preview") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            matchingFallbacks += listOf("debug")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            // A release without distribution credentials stays unsigned.
            signingConfig = if (releaseStore != null) signingConfigs.getByName("distribution") else null
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    testBuildType = providers.gradleProperty("testBuildType").getOrElse("debug")
    sourceSets.getByName("preview").java.srcDir("src/debug/java")
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17; isCoreLibraryDesugaringEnabled = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.maplibre.gl:android-sdk:11.13.5")
    implementation("com.stadiamaps.ferrostar:core:0.59.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.car.app:app:1.8.0-rc01")
    implementation("androidx.car.app:app-projected:1.8.0-rc01")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("androidx.car.app:app-testing:1.8.0-rc01") {
        // Robolectric belongs to local JVM tests. On a device it causes
        // ActivityScenario to select ShadowLooper without a sandbox.
        exclude(group = "org.robolectric")
    }
    debugImplementation("androidx.compose.ui:ui-tooling")
}
