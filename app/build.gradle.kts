import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val releaseVersionName: String = System.getenv("VERSION_NAME") ?: "0.2.0"
val releaseVersionCode: Int = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
val releaseKeystore: String? = System.getenv("KEYSTORE_FILE")
val compileSdkVersion: Int = System.getenv("COMPILE_SDK")?.toIntOrNull() ?: 37
val targetSdkVersion: Int = System.getenv("TARGET_SDK")?.toIntOrNull() ?: compileSdkVersion

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.tokinote.app"
    compileSdk = compileSdkVersion

    defaultConfig {
        applicationId = "com.tokinote.app"
        minSdk = 36
        targetSdk = targetSdkVersion
        versionCode = releaseVersionCode
        versionName = releaseVersionName
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
