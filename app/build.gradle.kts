plugins { alias(libs.plugins.android.application) }

android {
    namespace = "app.androglass"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.androglass"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-preview.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes { release { isMinifyEnabled = false } }
}

dependencies { compileOnly(libs.libxposed.api) }
