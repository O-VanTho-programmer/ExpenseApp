import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.antigravity.expensetracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.antigravity.expensetracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val keystoreConfigFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties()
    if (keystoreConfigFile.exists()) {
        keystoreConfigFile.inputStream().use { keystoreProps.load(it) }
    }

    val keystorePath: String? = System.getenv("KEYSTORE_PATH") ?: keystoreProps.getProperty("KEYSTORE_PATH")
    val keystorePass: String? = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProps.getProperty("KEYSTORE_PASSWORD")
    val keyAliasVal: String? = System.getenv("KEY_ALIAS") ?: keystoreProps.getProperty("KEY_ALIAS")
    val keyPass: String? = System.getenv("KEY_PASSWORD") ?: keystoreProps.getProperty("KEY_PASSWORD")

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }

        if (!keystorePath.isNullOrBlank() && !keystorePass.isNullOrBlank()) {
            val keyFile = if (file(keystorePath).isAbsolute) file(keystorePath) else rootProject.file(keystorePath)
            if (keyFile.exists()) {
                create("release") {
                    storeFile = keyFile
                    storePassword = keystorePass
                    keyAlias = if (!keyAliasVal.isNullOrBlank()) keyAliasVal else "expense"
                    keyPassword = if (!keyPass.isNullOrBlank()) keyPass else keystorePass
                    enableV1Signing = true
                    enableV2Signing = true
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseSigning = signingConfigs.findByName("release")
            signingConfig = releaseSigning ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    
    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room & SQLCipher
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

