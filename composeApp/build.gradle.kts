import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.kover)
}

kover {
    reports {
        filters {
            // 排除 JVM 单测结构性无法覆盖的代码，让覆盖率反映真实可测层
            excludes {
                packages(
                    // Compose UI —— 仅 androidDeviceTest（设备 UI 测试）可覆盖
                    "restarhalf.stellar.schedule.ui.screens",
                    "restarhalf.stellar.schedule.ui.components",
                    "restarhalf.stellar.schedule.ui.navigation",
                    "restarhalf.stellar.schedule.ui.effect",
                    "restarhalf.stellar.schedule.ui.blur",
                    "restarhalf.stellar.schedule.ui.icons",
                    "restarhalf.stellar.schedule.ui.modifier",
                    "restarhalf.stellar.schedule.ui.image",
                    "restarhalf.stellar.schedule.ui.impl",
                    // Android 运行时 / 生成代码 / 三方拷贝 / DI 装配
                    "restarhalf.stellar.schedule.widget",
                    "restarhalf.stellar.schedule.calendar",
                    "restarhalf.stellar.schedule.pictureselector",
                    "restarhalf.stellar.schedule.di",
                    "restarhalf.stellar.schedule.androidapp",
                    "restarhalf.stellar.schedule.config",
                    "restarhalf.stellar.schedule.data.local.dao",
                    // PDF 文件选择器胶水（@Composable + ActivityResult，仅设备可测）
                    "restarhalf.stellar.schedule.papers",
                    "chrnova.composeapp.generated.resources",
                )
                // 根包 AppRoot/AppContent/AppState（Compose 入口）
                classes("restarhalf.stellar.schedule.App*")
                // androidMain 运行时胶水 + Compose 编译器生成
                classes(
                    "restarhalf.stellar.schedule.AndroidApp*",
                    "restarhalf.stellar.schedule.MainActivity*",
                    "restarhalf.stellar.schedule.CourseSelectionService*",
                    "restarhalf.stellar.schedule.ComposableSingletons*",
                )
            }
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

composeCompiler {
    stabilityConfigurationFiles.add(
        project.layout.projectDirectory.file("compose_compiler_config.conf")
    )
}

val localProps = Properties().apply {
    runCatching { rootProject.file("local.properties").inputStream().use(::load) }
}

val localSecretsSignKey =
    (localProps.getProperty("SIGN_KEY") ?: System.getenv("SIGN_KEY"))
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: error("SIGN_KEY missing in local.properties or environment")
val localSecretsAesKey =
    (localProps.getProperty("AES_KEY") ?: System.getenv("AES_KEY"))
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: error("AES_KEY missing in local.properties or environment")


require(localSecretsAesKey.toByteArray(Charsets.UTF_8).size == 16) {
    "AES_KEY must be exactly 16 bytes for AES-128"
}

val generatedLocalSecretsDir = layout.buildDirectory.dir("generated/source/localSecrets/kotlin")

abstract class GenerateLocalSecretsTask : DefaultTask() {
    @get:Input
    abstract val aesKey: Property<String>

    @get:Input
    abstract val signKey: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputFile =
            outputDir.file("restarhalf/stellar/schedule/config/LocalSecrets.kt").get().asFile

        fun String.toHexStringArray(): String {
            val sb = StringBuilder()
            for ((i, c) in this.withIndex()) {
                if (i > 0) sb.append(", ")
                sb.append("0x%04x".format(c.code))
            }
            return sb.toString()
        }

        val aesChars = aesKey.get().toHexStringArray()
        val signChars = signKey.get().toHexStringArray()

        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package restarhalf.stellar.schedule.config

            internal object LocalSecrets {
                private val _a = intArrayOf($aesChars)
                private val _s = intArrayOf($signChars)

                val AES_KEY: String by lazy { _a.joinToString("") { it.toChar().toString() } }
                val SIGN_KEY: String by lazy { _s.joinToString("") { it.toChar().toString() } }
            }
            """.trimIndent()
        )
    }
}

val generateLocalSecrets = tasks.register<GenerateLocalSecretsTask>("generateLocalSecrets") {
    description = "生成LocalSecrets"
    aesKey.set(localSecretsAesKey)
    signKey.set(localSecretsSignKey)
    outputDir.set(generatedLocalSecretsDir)
}

tasks.matching { it.name.startsWith("compile") || it.name.startsWith("ksp") }.configureEach {
    dependsOn(generateLocalSecrets)
}

kotlin {
    compilerOptions {

        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "restarhalf.stellar.schedule"
        compileSdk { version = release(37) }
        minSdk = 28

        androidResources {
            enable = true
        }

        withHostTest {}

        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }


    iosArm64 {
        binaries.framework {
            export(libs.lifecycle.viewmodel)
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "restarhalf.stellar.schedule.composeapp")
            if (buildType == NativeBuildType.RELEASE) {
                binaryOption("smallBinary", "true")
            }
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generatedLocalSecretsDir)
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.animation)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.androidx.navigationevent)
                implementation(libs.miuix.ui)
                implementation(libs.miuix.blur)
                implementation(libs.miuix.preference)
                implementation(libs.miuix.nav)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor3)
                implementation(libs.markdown.renderer)
                implementation(libs.markdown.renderer.coil3)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.settings.coroutines)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.room3.runtime)
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.qrose)
                implementation(libs.cryptography.core)
                implementation(libs.cryptography.provider.optimal)
                implementation(libs.lifecycle.runtime.compose)
                implementation(libs.kotlinx.collections.immutable)
                api(libs.lifecycle.viewmodel)
            }
        }
        androidMain.dependencies {
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.glance.appwidget)
            implementation(libs.androidx.glance)
            implementation(libs.androidx.exifinterface)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.ktor.client.mock)
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.ultron.compose)
                implementation(libs.androidx.test.runner)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

dependencies {
    kspAndroid(libs.room3.compiler)
    add("kspIosArm64", libs.room3.compiler)
}