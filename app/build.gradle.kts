plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val gitBuildNumber: Int = try {
    val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
        .directory(project.rootDir)
        .start()
    process.inputStream.bufferedReader().readText().trim().toIntOrNull() ?: 1
} catch (e: Exception) {
    1
}

val baseVersionName = "0.1.6"
val fullVersionName = "$baseVersionName-b$gitBuildNumber"

android {
    namespace = "com.programmerkeyboard"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.programmerkeyboard"
        minSdk = 24
        targetSdk = 34
        versionCode = maxOf(gitBuildNumber, 100)
        versionName = "$baseVersionName-b${maxOf(gitBuildNumber, 100)}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "programmer123"
            keyAlias = "programmer_keyboard"
            keyPassword = "programmer123"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "infinikey-ime-v${versionName}-${buildType.name}.apk"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.gson)
}

tasks.register<Exec>("generateEmojiLayouts") {
    workingDir = project.rootDir
    commandLine = listOf("python3", "scripts/generate_emoji_layouts.py")
}

tasks.named("preBuild") {
    dependsOn("generateEmojiLayouts")
}
