import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.datn"
    compileSdk = 36
    buildFeatures {
        compose = true
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.example.datn"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        val localProps = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localProps.load(localFile.inputStream())
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val minioEndpoint = localProps.getProperty("MINIO_ENDPOINT") ?: ""
        val minioAccessKey = localProps.getProperty("MINIO_ACCESS_KEY") ?: ""
        val minioSecretKey = localProps.getProperty("MINIO_SECRET_KEY") ?: ""
        val minioBucket = localProps.getProperty("MINIO_BUCKET") ?: ""
        buildConfigField("String", "MINIO_ENDPOINT", "\"$minioEndpoint\"")
        buildConfigField("String", "MINIO_ACCESS_KEY", "\"$minioAccessKey\"")
        buildConfigField("String", "MINIO_SECRET_KEY", "\"$minioSecretKey\"")
        buildConfigField("String", "MINIO_BUCKET", "\"$minioBucket\"")
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-database-ktx")

    // MinIO Client
    implementation("io.minio:minio:8.5.7")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation(libs.androidx.hilt.common)
    implementation(libs.firebase.firestore)
    ksp("androidx.room:room-compiler:2.6.1")

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.navigation.common.ktx)

    // Jetpack Compose BOM
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-compiler:2.57.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // XML parsing
    // Thư viện XML Stream API (bản tương thích Android)
    implementation("stax:stax-api:1.0.1")
    // Thư viện XML parser (Aalto — thay thế com.bea)
    implementation("com.fasterxml:aalto-xml:1.2.2")
    // JmDNS
    implementation("org.jmdns:jmdns:3.5.9")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Nếu anh dùng Hilt để inject Worker
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.kotlinx.serialization.json)
    // ✅ Thêm Jackson Kotlin Module
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    // Thêm Jackson Core (thường được pull theo, nhưng thêm cho chắc)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
}