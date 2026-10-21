import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

android {
    namespace = "com.wolfcola.equatecontinued"
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        viewBinding = true
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "${rootProject.projectDir}/release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: "equate"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    defaultConfig {
        applicationId = "com.wolfcola.equatecontinued"
        minSdk = 33
        targetSdk = 37
        versionCode = 29
        versionName = "2.2.20"

        testApplicationId = "com.wolfcola.equatecontinued.test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    implementation(libs.material)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.constraintlayout)
    // only the debug SimpleIdlingResource talks to Espresso; release gets a no-op
    debugImplementation(libs.espresso.idling.resource)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.json)

    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.guava)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.contrib)
    androidTestImplementation(libs.espresso.idling.resource)
}
