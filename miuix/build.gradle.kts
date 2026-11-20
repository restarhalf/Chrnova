
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "restarhalf.stellar.schedule.miuix"
        compileSdk { version = release(37) }
        minSdk = 28
    }

    iosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.material3)
            api(libs.softglass)
            api(libs.materialkolor.utilities)
            api(libs.jetbrains.annotations)

            implementation(libs.miuix.core)
            implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.4") {
                exclude(group = "top.yukonga.miuix.kmp", module = "miuix-ui")
            }
            implementation("top.yukonga.miuix.kmp:miuix-blur:0.9.4") {
                exclude(group = "top.yukonga.miuix.kmp", module = "miuix-ui")
            }
            implementation("top.yukonga.miuix.kmp:miuix-navigation3-ui:0.9.3") {
                exclude(group = "top.yukonga.miuix.kmp", module = "miuix-squircle")
            }
            implementation(libs.miuix.squircle)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.navigationevent)
        }

        iosMain {
            kotlin.srcDir("src/skikoMain/kotlin")
        }
    }
}
