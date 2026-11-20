plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "restarhalf.stellar.schedule.pictureselector"
        compileSdk { version = release(37) }
        minSdk = 28

        androidResources {
            enable = true
        }
    }

    iosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":ui-components"))
            implementation(project(":miuix"))
            api(libs.compose.runtime)
            api(libs.compose.ui)
            implementation(libs.compose.animation)
            implementation(libs.compose.foundation)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.navigationevent)
            implementation(libs.coil.compose)
            implementation(libs.miuix.core)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.exifinterface)
        }
    }
}
