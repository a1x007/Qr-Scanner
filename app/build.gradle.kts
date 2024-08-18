
import java.util.regex.Pattern.compile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

android {
    namespace = "com.ashique.qrscanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ashique.qrscanner"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        renderscriptSupportModeBlasEnabled = true
        renderscriptNdkModeEnabled = true
        renderscriptTargetApi = 19
        renderscriptSupportModeEnabled = true
        // for chaquopy
        ndk {
            // On Apple silicon, you can omit x86_64.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

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

    buildFeatures {
        viewBinding = true

    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // for chaquopy
    flavorDimensions += "pyVersion"
    productFlavors {
        create("py310") { dimension = "pyVersion" }
        create("py311") { dimension = "pyVersion" }
    }
    ndkVersion = "26.1.10909125"
}


chaquopy {
    productFlavors {
        getByName("py310") { version = "3.10" }
        getByName("py311") { version = "3.11" }
    }

    sourceSets {

    }

    defaultConfig {
        pip {
            install("pillow")
        }
    }
}


dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("com.airbnb.android:lottie:6.4.0")

    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")

    implementation("com.google.zxing:core:3.5.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")



    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")


    implementation("com.isseiaoki:simplecropview:1.1.8")


    implementation("com.github.bumptech.glide:glide:4.16.0")

    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")


    implementation("io.github.waynejo:androidndkgif:1.0.1")
    implementation("com.github.awxkee:aire:0.13.12")

    implementation(project(":custom_qr_generator"))
}