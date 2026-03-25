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
val forceBuildChannel = (findProperty("BUILD_CHANNEL") as? String)?.trim()?.uppercase()
val ciRunNumberOverride = (findProperty("CI_RUN_NUMBER") as? String)?.trim()
val ciRunNumberFromEnv = System.getenv("GITHUB_RUN_NUMBER")?.trim()
val isCiBuild =
    forceBuildChannel == "CI" ||
            System.getenv("GITHUB_ACTIONS")?.equals("true", ignoreCase = true) == true
val ciRunNumber = ciRunNumberOverride?.takeIf { it.isNotEmpty() }
    ?: ciRunNumberFromEnv?.takeIf { it.isNotEmpty() }
val resolvedVersionName =
    if (isCiBuild) "$releaseVersionName-ci.${ciRunNumber ?: "local"}" else releaseVersionName
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
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "restarhalf.stellar.schedule"
        minSdk = 30
        targetSdk = 36
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
        checkReleaseBuilds = false
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
}
dependencies {
    implementation(project(":composeApp"))
}

