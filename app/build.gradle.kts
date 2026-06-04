plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.minlish.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.minlish.app"
        minSdk = 26
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
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    // ======= DEFAULT (giữ nguyên từ template) =======
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // ======= FIREBASE =======
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // ======= GOOGLE SIGN-IN =======
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // ======= NAVIGATION =======
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // ======= VIEWMODEL =======
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // ======= WORKMANAGER =======
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // ======= MATERIAL ICONS EXTENDED =======
    implementation("androidx.compose.material:material-icons-extended")

    // ======= COROUTINES (cho Firebase .await()) =======
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // ======= EXCEL export (import uses SimpleXlsxReader — Android-safe) =======
    implementation("org.dhatim:fastexcel:0.18.0")

    // ======= TEST (giữ nguyên) =======
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
