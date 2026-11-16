import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val releaseSigningVariableNames = listOf(
    "INFINIKEY_RELEASE_STORE_FILE",
    "INFINIKEY_RELEASE_STORE_PASSWORD",
    "INFINIKEY_RELEASE_KEY_ALIAS",
    "INFINIKEY_RELEASE_KEY_PASSWORD"
)
val releaseSigningProperties = Properties()
val releaseSigningPropertiesFile = listOf(
    rootProject.file("build/release-signing.properties"),
    rootProject.file("release-signing.properties")
).firstOrNull { it.isFile }
releaseSigningPropertiesFile?.reader(Charsets.UTF_8)?.use { reader ->
    releaseSigningProperties.load(reader)
}
val releaseSigningValues = releaseSigningVariableNames.associateWith { name ->
    releaseSigningProperties.getProperty(name)?.takeIf { value -> value.isNotBlank() }
        ?: providers.gradleProperty(name).orNull?.takeIf { value -> value.isNotBlank() }
        ?: System.getenv(name)?.takeIf { value -> value.isNotBlank() }
}
val releaseSigningConfigured = releaseSigningValues.values.any { it != null }
val missingReleaseSigningVariables = releaseSigningValues.filterValues { it == null }.keys

if (releaseSigningConfigured && missingReleaseSigningVariables.isNotEmpty()) {
    throw org.gradle.api.GradleException(
        "Release signing is incomplete. Set all of: ${releaseSigningVariableNames.joinToString()}. " +
            "Use environment variables, Gradle project properties, or an untracked release-signing.properties file."
    )
}

val releaseStoreFile = releaseSigningValues["INFINIKEY_RELEASE_STORE_FILE"]
    ?.let { rootProject.file(it) }
if (releaseSigningConfigured && (releaseStoreFile == null || !releaseStoreFile.isFile)) {
    throw org.gradle.api.GradleException(
        "INFINIKEY_RELEASE_STORE_FILE does not point to an existing keystore file."
    )
}

android {
    namespace = "com.infinikey_ime"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.infinikey_ime"
        minSdk = 24
        targetSdk = 34
        versionCode = 277
        versionName = "0.3.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (releaseSigningConfigured) {
        signingConfigs {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseSigningValues.getValue("INFINIKEY_RELEASE_STORE_PASSWORD")
                keyAlias = releaseSigningValues.getValue("INFINIKEY_RELEASE_KEY_ALIAS")
                keyPassword = releaseSigningValues.getValue("INFINIKEY_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
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

// Gradle does not include external signing inputs in packageRelease's output
// fingerprint. Always package again so an APK from another signing mode or key
// cannot be reused as the current release artifact.
tasks.configureEach {
    if (name == "packageRelease") {
        outputs.upToDateWhen { false }
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
    commandLine = listOf("python3", "scripts/generate_emoji_layouts.py", "--version", android.defaultConfig.versionName ?: "0.3.7")
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



