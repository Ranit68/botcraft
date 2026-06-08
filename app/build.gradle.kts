plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.ranit.botscraft"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ranit.botscraft"
        minSdk = 24
        targetSdk = 35
        versionCode = 11
        versionName = "1.10"

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
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.android.billingclient:billing:7.1.1")
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-firestore")
    implementation(libs.firebase.database)
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.airbnb.android:lottie:6.7.1")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation(libs.photoview)
    implementation(libs.biometric)
    implementation("androidx.work:work-runtime:2.9.1")
    implementation("com.google.guava:guava:31.1-android")
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
