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

val baseVersionName = "0.2.32"
val fullVersionName = "$baseVersionName-b$gitBuildNumber"

android {
    namespace = "com.infinikey_ime"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.infinikey_ime"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
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
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
}


tasks.register<Exec>("generateEmojiLayouts") {
    workingDir = project.rootDir
    commandLine = listOf("python3", "scripts/generate_emoji_layouts.py", "--version", baseVersionName)
}

tasks.register<Exec>("splitKeyClicks") {
    workingDir = project.rootDir
    inputs.dir(file("${project.rootDir}/app/src/main/assets/audio"))
    inputs.file(file("${project.rootDir}/scripts/split_key_clicks.py"))
    outputs.dir(file("${project.rootDir}/app/src/main/assets/audio_split"))
    commandLine = listOf("python3", "scripts/split_key_clicks.py")
}

tasks.named("preBuild") {
    dependsOn("generateEmojiLayouts", "splitKeyClicks")
}



