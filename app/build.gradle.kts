import java.util.Base64

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

val ksB64: String? = System.getenv("KEYSTORE_BASE64")
val ksFile = layout.buildDirectory.file("release.jks").get().asFile
if (!ksB64.isNullOrBlank()) { ksFile.parentFile.mkdirs(); ksFile.writeBytes(Base64.getDecoder().decode(ksB64)) }

android {
    namespace = "ru.miroslav.vpnauto"
    compileSdk = 34
    defaultConfig { applicationId = "ru.miroslav.vpnauto"; minSdk = 26; targetSdk = 34; versionCode = 7; versionName = "2.2" }
    signingConfigs {
        create("release") {
            if (!ksB64.isNullOrBlank()) {
                storeFile = ksFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            // Пока ключ не добавлен в Secrets — подписываем debug-ключом, чтобы сборка не падала.
            signingConfig = if (!ksB64.isNullOrBlank()) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { viewBinding = true }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
