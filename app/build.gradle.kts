plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
val keyFile = providers.environmentVariable("FRUIT_CATCHER_KEYSTORE")
android {
    namespace = "com.fruitcatcher.fruit"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.fruitcatcher.fruit"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
    if (keyFile.isPresent) {
        signingConfigs.create("production") {
            storeFile = file(keyFile.get())
            storePassword = providers.environmentVariable("FRUIT_CATCHER_STORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("FRUIT_CATCHER_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("FRUIT_CATCHER_KEY_PASSWORD").get()
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("production")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    testImplementation("junit:junit:4.13.2")
}
