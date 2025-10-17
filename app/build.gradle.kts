plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Apply Google Services plugin only if google-services.json exists
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "com.shoecycle"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shoecycle"
        minSdk = 26
        targetSdk = 35
        versionCode = 15
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Room schema export configuration
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release-keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = "shoecycle"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("boolean", "USE_MOCK_SERVICES", "true")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "USE_MOCK_SERVICES", "false")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Task to remove AD_ID permissions from merged manifest
tasks.register("stripAdIdPermissions") {
    doLast {
        val manifestFiles = fileTree("${buildDir}/intermediates/merged_manifests/") {
            include("**/AndroidManifest.xml")
        }

        manifestFiles.forEach { file ->
            val content = file.readText()
            val modifiedContent = content
                .replace("""<uses-permission android:name="com.google.android.gms.permission.AD_ID" />""", "<!-- AD_ID permission removed -->")
                .replace("""<uses-permission android:name="android.permission.ACCESS_ADSERVICES_AD_ID" />""", "<!-- ACCESS_ADSERVICES_AD_ID permission removed -->")
                .replace("""<uses-permission android:name="android.permission.ACCESS_ADSERVICES_ATTRIBUTION" />""", "<!-- ACCESS_ADSERVICES_ATTRIBUTION permission removed -->")

            if (content != modifiedContent) {
                file.writeText(modifiedContent)
                println("Removed AD_ID permissions from: ${file.path}")
            }
        }
    }
}

// Hook the strip task to run after manifest merging
afterEvaluate {
    tasks.named("processReleaseManifest") {
        finalizedBy("stripAdIdPermissions")
    }
    tasks.named("processDebugManifest") {
        finalizedBy("stripAdIdPermissions")
    }

    // Also hook it to bundle tasks for Play Store uploads
    tasks.matching { it.name.contains("bundle", ignoreCase = true) }.configureEach {
        dependsOn("stripAdIdPermissions")
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Activity Compose
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // AppCompat for themes
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // DataStore (for preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Room database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Accompanist for permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // Health Connect
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")
    
    // Security - for EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}