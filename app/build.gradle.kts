plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.safeway"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.safeway"
        minSdk = 24
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
}

dependencies {
    // ───────────────────────────────────────────────
    // AndroidX / UI 라이브러리
    implementation(libs.appcompat)             // androidx.appcompat:appcompat
    implementation(libs.material)              // com.google.android.material:material
    implementation(libs.activity)              // androidx.activity:activity-ktx
    implementation(libs.constraintlayout)      // androidx.constraintlayout:constraintlayout

    // ───────────────────────────────────────────────
    // 네이버 지도 SDK
    implementation(libs.map.sdk)               // com.naver.maps:map-sdk:3.x.x
    // FusedLocationSource (현재 위치 받기 위해 Play Services Location)
    implementation(libs.play.services.location) // com.google.android.gms:play-services-location:<latest>

    // ───────────────────────────────────────────────
    // 네트워크 통신 (Place Search / 길찾기 API 호출)
    implementation(libs.okhttp)                // com.squareup.okhttp3:okhttp:<latest>
    implementation(libs.logging.interceptor)   // com.squareup.okhttp3:logging-interceptor:<latest>

    // ───────────────────────────────────────────────
    // Test 라이브러리
    testImplementation(libs.junit)             // junit:junit:4.13.2 (등등)
    androidTestImplementation(libs.ext.junit)  // androidx.test.ext:junit:1.1.5
    androidTestImplementation(libs.espresso.core) // androidx.test.espresso:espresso-core:3.5.1
}
