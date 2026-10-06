plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

android {
    namespace = "com.nexa.finance"
    val apiUrl = providers.gradleProperty("nexaApiUrl").orNull ?: "http://10.0.2.2:8080"
    val escapedApiUrl = apiUrl.replace("\\", "\\\\").replace("\"", "\\\"")
    val versionCodeValue = providers.gradleProperty("nexaVersionCode").orNull?.toIntOrNull() ?: 1
    val versionNameValue = providers.gradleProperty("nexaVersionName").orNull ?: "0.1.0"

    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "com.nexa.finance"
        minSdk = 26
        targetSdk = 37
        versionCode = versionCodeValue
        versionName = versionNameValue
        buildConfigField("String", "NEXA_API_URL", "\"$escapedApiUrl\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "NEXA_API_URL", "\"$escapedApiUrl\"")
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val releaseApiUrl = providers.gradleProperty("nexaReleaseApiUrl").orNull
                ?: "https://nexa-api-production-6cc5.up.railway.app"
            val escapedReleaseApiUrl = releaseApiUrl.replace("\\", "\\\\").replace("\"", "\\\"")
            buildConfigField("String", "NEXA_API_URL", "\"$escapedReleaseApiUrl\"")
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    lint {
        // local.properties is machine-local and ignored by Git; its Windows
        // path triggers a PropertyEscape false positive in Android Lint.
        disable += "PropertyEscape"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.work:work-runtime-ktx:2.11.0")

    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    implementation("net.zetetic:sqlcipher-android:4.19.0")
    implementation("androidx.sqlite:sqlite:2.7.1")
    ksp("androidx.room:room-compiler:2.8.5")
    testImplementation("androidx.room:room-testing:2.8.5")
    androidTestImplementation("androidx.room:room-testing:2.8.5")
    androidTestImplementation("androidx.work:work-testing:2.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.4.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
