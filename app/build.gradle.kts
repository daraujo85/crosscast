import java.util.Properties
import java.io.FileInputStream

val keystoreFile = project.rootProject.file("local.properties")
val properties = Properties()
if (keystoreFile.exists()) {
    properties.load(FileInputStream(keystoreFile))
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.campilot"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.campilot"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            keyAlias = properties.getProperty("KEY_ALIAS")
            keyPassword = properties.getProperty("KEY_PASSWORD")
            storeFile = if (properties.getProperty("KEYSTORE_FILE") != null) file(properties.getProperty("KEYSTORE_FILE")) else null
            storePassword = properties.getProperty("STORE_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (properties.getProperty("KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // Ktor Server
    implementation(libs.ktor.core)
    implementation(libs.ktor.netty)
    implementation(libs.ktor.negotiation)
    implementation(libs.ktor.json)

    // QR Code
    implementation(libs.zxing)

    // Room (persistência de cenas/fontes do Modo Estúdio) - desabilitado temporariamente
    // implementation(libs.room.runtime)
    // implementation(libs.room.ktx)
    // kapt(libs.room.compiler)
}
