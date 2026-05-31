import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    val localProperties =
        Properties().apply {
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { load(it) }
            }
        }
    val naverMapMcpId =
        (
            (project.findProperty("NAVER_MAP_MCP_ID") as String?)
                ?: localProperties.getProperty("NAVER_MAP_MCP_ID", "")
        ).trim()
    val escapedNaverMapMcpId =
        naverMapMcpId
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
    val functionsBaseUrl =
        (
            (project.findProperty("FUNCTIONS_BASE_URL") as String?)
                ?: localProperties.getProperty(
                    "FUNCTIONS_BASE_URL",
                    "https://us-central1-focustation-f7fe9.cloudfunctions.net",
                )
        ).trim().trimEnd('/')
    val escapedFunctionsBaseUrl =
        functionsBaseUrl
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
    val naverSearchClientId =
        (
            (project.findProperty("NAVER_SEARCH_CLIENT_ID") as String?)
                ?: localProperties
                    .getProperty("NAVER_SEARCH_CLIENT_ID", "")
                    .ifBlank { localProperties.getProperty("NAVER_SEARCH_CLIENTID", "") }
                    .ifBlank { localProperties.getProperty("NAVER_CLIENT_ID", "") }
        ).trim()
    val escapedNaverSearchClientId =
        naverSearchClientId
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
    val naverSearchClientSecret =
        (
            (project.findProperty("NAVER_SEARCH_CLIENT_SECRET") as String?)
                ?: localProperties
                    .getProperty("NAVER_SEARCH_CLIENT_SECRET", "")
                    .ifBlank { localProperties.getProperty("NAVER_SEARCH_CLIENTSECRET", "") }
                    .ifBlank { localProperties.getProperty("NAVER_CLIENT_SECRET", "") }
        ).trim()
    val escapedNaverSearchClientSecret =
        naverSearchClientSecret
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

    namespace = "net.focustation.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.focustation.myapplication"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["NAVER_MAP_MCP_ID"] = naverMapMcpId
        buildConfigField("String", "NAVER_MAP_MCP_ID", "\"$escapedNaverMapMcpId\"")
        buildConfigField("String", "FUNCTIONS_BASE_URL", "\"$escapedFunctionsBaseUrl\"")
        buildConfigField("String", "NAVER_SEARCH_CLIENT_ID", "\"$escapedNaverSearchClientId\"")
        buildConfigField("String", "NAVER_SEARCH_CLIENT_SECRET", "\"$escapedNaverSearchClientSecret\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation(libs.naver.maps)
    implementation(libs.playServicesAuth)
    implementation(platform(libs.firebaseBom))
    implementation(libs.firebaseAuth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil.compose)
    implementation(libs.lottie.compose)
    implementation("androidx.appcompat:appcompat:1.6.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

ktlint {
    version.set("1.4.1")
    android.set(true)
    outputColorName.set("RED")
    // Compose @Composable 함수는 PascalCase를 허용
    filter {
        exclude("**/generated/**")
    }
}
