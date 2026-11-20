plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "restarhalf.stellar.schedule.uicomponents"
        compileSdk { version = release(37) }
        minSdk = 28
    }

    iosArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":miuix"))
            api(libs.softglass)
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.compose.animation)
            implementation(libs.miuix.core)
        }
    }
}
