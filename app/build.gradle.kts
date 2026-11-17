import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// Release signing: read from keystore.properties (git-ignored) or environment variables.
// With neither present the release build is left unsigned, so a plain clone still builds.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(property: String, environment: String): String? =
    keystoreProperties.getProperty(property) ?: System.getenv(environment)

// Backend URL. Set it in local.properties (apiBaseUrl=...), with -PapiBaseUrl=..., or with the
// API_BASE_URL environment variable. Debug builds can use a different one (apiBaseUrlDebug,
// API_BASE_URL_DEBUG), for example http://10.0.2.2:8080/api/v1/ for a server on the host machine.
// Without any of them the placeholder is used and the app runs offline-only.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun configValue(property: String, environment: String): String? =
    (project.findProperty(property) as String?)?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(property)?.takeIf { it.isNotBlank() }
        ?: System.getenv(environment)?.takeIf { it.isNotBlank() }

fun normalizedApiUrl(raw: String): String {
    val url = raw.trim()
    require(url.startsWith("https://") || url.startsWith("http://")) {
        "API base URL must start with http:// or https:// but was '$url'"
    }
    return if (url.endsWith("/")) url else "$url/"
}

val placeholderApiUrl = "https://your-api-url.com/api/v1/"
val apiBaseUrl = normalizedApiUrl(configValue("apiBaseUrl", "API_BASE_URL") ?: placeholderApiUrl)
val apiBaseUrlDebug = configValue("apiBaseUrlDebug", "API_BASE_URL_DEBUG")?.let(::normalizedApiUrl) ?: apiBaseUrl

ksp {
    // Export Room schemas so database migrations can be reviewed and tested.
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.triplog"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.triplog"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    signingConfigs {
        val storeFilePath = signingValue("storeFile", "KEYSTORE_FILE")
        if (storeFilePath != null) {
            create("release") {
                storeFile = rootProject.file(storeFilePath)
                storePassword = signingValue("storePassword", "KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrlDebug\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)

    // WorkManager
    implementation(libs.work.runtime.ktx)


    // Image loading
    implementation(libs.coil)
    implementation(libs.exifinterface)


    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
