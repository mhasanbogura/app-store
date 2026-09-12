plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.mahmuduls.appstore"
    minSdk = 24
    targetSdk = 36
    versionCode = getVersionCode()
    versionName = getVersionName()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debugConfig")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation(libs.androidx.work.runtime)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

fun getVersionCode(): Int {
    return project.rootProject.file("version.properties").useLines { lines ->
        lines.firstOrNull { it.startsWith("versionCode=") }
            ?.substringAfter("=")?.trim()?.toIntOrNull() ?: 1
    }
}
fun getVersionName(): String {
    return project.rootProject.file("version.properties").useLines { lines ->
        lines.firstOrNull { it.startsWith("versionName=") }
            ?.substringAfter("=")?.trim() ?: "1.0.0"
    }
}

tasks.register("publishToDrive") {
    doLast {
        val variantDir = file("build/outputs/apk/debug")
        val driveDir = file("/home/mhasanbogura/Google Drive/mahmudulhasandhk70/Apk Store")
        if (!driveDir.exists()) driveDir.mkdirs()
        val verFile = project.rootProject.file("version.properties")
        val verName = verFile.useLines { lines ->
            lines.firstOrNull { it.startsWith("versionName=") }
                ?.substringAfter("=")?.trim() ?: "1.0.0"
        }
        val verCode = verFile.useLines { lines ->
            lines.firstOrNull { it.startsWith("versionCode=") }
                ?.substringAfter("=")?.trim() ?: "1"
        }
        val newName = "App Store_com.mahmuduls.appstore_v${verName}_build_${verCode}.apk"
        val srcFile = file("${variantDir}/app-debug.apk")
        if (srcFile.exists()) {
            srcFile.copyTo(File(driveDir, newName), overwrite = true)
            logger.lifecycle("Published to Drive: ${driveDir}/${newName}")
            driveDir.listFiles()?.filter { it.name.startsWith("App Store_com.mahmuduls.appstore_v") && it.name != newName }?.forEach { old ->
                old.delete()
                logger.lifecycle("Deleted old: ${old.name}")
            }
        } else {
            logger.warn("APK not found: ${srcFile.absolutePath}")
        }
    }
}

tasks.register("incrementVersion") {
    dependsOn("publishToDrive")
    val verFile = project.rootProject.file("version.properties")
    doLast {
        val lines = verFile.readText().lines().toMutableList()
        var newCode = 1
        for (i in lines.indices) {
            if (lines[i].startsWith("versionCode=")) {
                val code = lines[i].substringAfter("=").trim().toIntOrNull() ?: 1
                newCode = code + 3
                lines[i] = "versionCode=${newCode}"
            }
        }
        for (i in lines.indices) {
            if (lines[i].startsWith("versionName=")) {
                val parts = lines[i].substringAfter("=").trim().split(".").toMutableList()
                while (parts.size < 3) parts.add("0")
                var idx = parts.size - 1
                var carry = 1
                while (idx >= 0 && carry > 0) {
                    val n = (parts[idx].toIntOrNull() ?: 0) + carry
                    parts[idx] = "${n % 100}"
                    carry = n / 100
                    idx--
                }
                if (carry > 0) parts.add(0, "${carry}")
                lines[i] = "versionName=" + parts.take(3).joinToString(".")
            }
        }
        verFile.writeText(lines.joinToString("\n") + "\n")
    }
}

tasks.matching { it.name.startsWith("assemble") }.configureEach {
    finalizedBy("publishToDrive", "incrementVersion")
}


