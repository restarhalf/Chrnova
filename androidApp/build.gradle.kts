import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
}
val localProps = Properties().apply {
    runCatching { load(rootProject.file("local.properties").inputStream()) }
}
val appVersionProps = Properties().apply {
    load(rootProject.file("iosApp/Configuration/AppVersion.xcconfig").inputStream())
}

val releaseVersionName =
    appVersionProps.getProperty("APP_VERSION_NAME")
        ?.takeIf { it.isNotBlank() }
        ?: error("APP_VERSION_NAME missing in iosApp/Configuration/AppVersion.xcconfig")
val releaseVersionCode =
    appVersionProps.getProperty("APP_VERSION_CODE")
        ?.trim()
        ?.toIntOrNull()
        ?: error("APP_VERSION_CODE missing or invalid in iosApp/Configuration/AppVersion.xcconfig")
val githubRef = System.getenv("GITHUB_REF")?.trim()
val githubRefType = System.getenv("GITHUB_REF_TYPE")?.trim()?.lowercase()
val githubRefName = System.getenv("GITHUB_REF_NAME")?.trim()
val githubTagName =
    when {
        githubRefType == "tag" && !githubRefName.isNullOrBlank() -> githubRefName
        githubRef?.startsWith("refs/tags/") == true ->
            githubRef.removePrefix("refs/tags/").trim().ifEmpty { null }
        else -> null
    }
fun resolveGitShortHash(): String? {
    val head = rootProject.file(".git/HEAD").takeIf { it.isFile }?.readText()?.trim().orEmpty()
    if (head.isBlank()) return null

    val fullHash =
        if (head.startsWith("ref:")) {
            val refPath = head.removePrefix("ref:").trim()
            val refFile = rootProject.file(".git/$refPath")
            if (refFile.isFile) {
                refFile.readText().trim()
            } else {
                rootProject.file(".git/packed-refs")
                    .takeIf { it.isFile }
                    ?.readLines()
                    ?.firstOrNull { line ->
                        line.isNotBlank() &&
                            !line.startsWith("#") &&
                            !line.startsWith("^") &&
                            line.endsWith(" $refPath")
                    }
                    ?.substringBefore(' ')
                    ?.trim()
            }
        } else {
            head
        }

    return fullHash
        ?.takeIf { it.matches(Regex("[0-9a-fA-F]{7,40}")) }
        ?.take(7)
        ?.lowercase()
}
val commitHashCode =
    System.getenv("GITHUB_SHA")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.take(7)
        ?: resolveGitShortHash()
        ?: "local"
val alphaVersionName = "${releaseVersionName}Alpha($commitHashCode)"
val resolvedVersionName = githubTagName ?: alphaVersionName
val resolvedVersionCode = releaseVersionCode


android {
    signingConfigs {
        getByName("debug") {
            storeFile = localProps["KEYSTORE_PATH"]?.let { file(it) }
                ?: System.getenv("KEYSTORE_PATH")?.let { file(it) }
            storePassword = localProps["KEYSTORE_PASS"] as? String ?: System.getenv("KEYSTORE_PASS")
            keyAlias = localProps["KEY_ALIAS"] as? String ?: System.getenv("KEY_ALIAS")
            keyPassword = localProps["KEY_PASSWORD"] as? String ?: System.getenv("KEY_PASSWORD")
        }
        create("release") {
            storeFile = localProps["KEYSTORE_PATH"]?.let { file(it) }
                ?: System.getenv("KEYSTORE_PATH")?.let { file(it) }
            storePassword = localProps["KEYSTORE_PASS"] as? String ?: System.getenv("KEYSTORE_PASS")
            keyAlias = localProps["KEY_ALIAS"] as? String ?: System.getenv("KEY_ALIAS")
            keyPassword = localProps["KEY_PASSWORD"] as? String ?: System.getenv("KEY_PASSWORD")
        }
    }
    namespace = "restarhalf.stellar.schedule.androidapp"
    compileSdk = 37

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "restarhalf.stellar.schedule"
        minSdk = 24
        targetSdk = 37
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = false
    }
    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    compileSdkMinor = 0
}
dependencies {
    implementation(project(":composeApp"))
}

