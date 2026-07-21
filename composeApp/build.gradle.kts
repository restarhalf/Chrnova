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
}

room3 {
    schemaDirectory("$projectDir/schemas")
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

val localSecretsPapersBaseUrl =
    (localProps.getProperty("PAPERS_BASE_URL") ?: System.getenv("PAPERS_BASE_URL"))
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: error("PAPERS_BASE_URL missing in local.properties or environment")

require(localSecretsAesKey.toByteArray(Charsets.UTF_8).size == 16) {
    "AES_KEY must be exactly 16 bytes for AES-128"
}

val generatedLocalSecretsDir = layout.buildDirectory.dir("generated/source/localSecrets/kotlin")

abstract class GenerateLocalSecretsTask : DefaultTask() {
    @get:Input
    abstract val aesKey: Property<String>

    @get:Input
    abstract val signKey: Property<String>

    @get:Input
    abstract val papersBaseUrl: Property<String>

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
        val urlChars = papersBaseUrl.get().toHexStringArray()

        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package restarhalf.stellar.schedule.config

            internal object LocalSecrets {
                private val _a = intArrayOf($aesChars)
                private val _s = intArrayOf($signChars)
                private val _u = intArrayOf($urlChars)

                val AES_KEY: String by lazy { _a.joinToString("") { it.toChar().toString() } }
                val SIGN_KEY: String by lazy { _s.joinToString("") { it.toChar().toString() } }
                val PAPERS_BASE_URL: String by lazy { _u.joinToString("") { it.toChar().toString() } }
            }
            """.trimIndent()
        )
    }
}

val generateLocalSecrets = tasks.register<GenerateLocalSecretsTask>("generateLocalSecrets") {
    description = "生成LocalSecrets"
    aesKey.set(localSecretsAesKey)
    signKey.set(localSecretsSignKey)
    papersBaseUrl.set(localSecretsPapersBaseUrl)
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
        minSdk = 24

        androidResources {
            enable = true
        }
    }


    iosArm64 {
        binaries.framework {
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
                implementation(libs.miuix.blur)
                implementation(libs.miuix.preference)
                implementation(libs.miuix.navigation3.ui)
                implementation(libs.coil.compose)
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
    kspAndroid(libs.room3.compiler)
    add("kspIosArm64", libs.room3.compiler)
}