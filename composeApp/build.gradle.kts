import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.room3)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

val localProps = Properties().apply {
    runCatching { rootProject.file("local.properties").inputStream().use(::load) }
}

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

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputFile =
            outputDir.file("restarhalf/stellar/schedule/config/LocalSecrets.kt").get().asFile
        val escapedAesKey =
            aesKey.get()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("$", "\\$")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
			package restarhalf.stellar.schedule.config

			internal object LocalSecrets {
				const val AES_KEY = "$escapedAesKey"
			}
			""".trimIndent()
        )
    }
}

val generateLocalSecrets by tasks.registering(GenerateLocalSecretsTask::class) {
    aesKey.set(localSecretsAesKey)
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
        minSdk = 31

        androidResources {
            enable = true
        }
    }

    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name == "compose-group-mapping") {
                useVersion("2.3.20")
            }
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            export(libs.lifecycle.viewmodel)
            export(libs.lifecycle.viewmodel.navigation3)
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
                implementation (libs.backdrop)
                implementation(libs.compose.runtime)
                implementation(libs.compose.animation)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.androidx.navigation3.runtime)
                implementation(libs.miuix.ui)
                implementation(libs.miuix.preference)
                implementation(libs.miuix.navigation3.ui)
                implementation(libs.coil.compose)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.settings.coroutines)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.room3.runtime)
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.multiplatform.markdown.renderer)
                api(libs.lifecycle.viewmodel)
                api(libs.lifecycle.viewmodel.navigation3)
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
    }
}

dependencies {
    add("kspAndroid", libs.room3.compiler)
    add("kspIosArm64", libs.room3.compiler)
    add("kspIosSimulatorArm64", libs.room3.compiler)
}

