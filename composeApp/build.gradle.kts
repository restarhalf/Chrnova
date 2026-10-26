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

            excludes {
                packages(

                    "restarhalf.stellar.schedule.ui.screens",
                    "restarhalf.stellar.schedule.ui.components",
                    "restarhalf.stellar.schedule.ui.navigation",
                    "restarhalf.stellar.schedule.ui.effect",
                    "restarhalf.stellar.schedule.ui.blur",
                    "restarhalf.stellar.schedule.ui.icons",
                    "restarhalf.stellar.schedule.ui.modifier",
                    "restarhalf.stellar.schedule.ui.image",
                    "restarhalf.stellar.schedule.ui.impl",

                    "restarhalf.stellar.schedule.widget",
                    "restarhalf.stellar.schedule.calendar",
                    "restarhalf.stellar.schedule.pictureselector",
                    "restarhalf.stellar.schedule.di",
                    "restarhalf.stellar.schedule.androidapp",
                    "restarhalf.stellar.schedule.config",
                    "restarhalf.stellar.schedule.data.local.dao",

                    "restarhalf.stellar.schedule.papers",
                    "chrnova.composeapp.generated.resources",
                )

                classes("restarhalf.stellar.schedule.App*")

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

val architectureBaselineFile = layout.projectDirectory.file("architecture-baseline.txt")

abstract class VerifyLayeringTask : DefaultTask() {
    
    @get:InputFiles
    abstract val prodSources: ConfigurableFileCollection

    @get:InputFiles
    abstract val testSources: ConfigurableFileCollection

    @get:InputFile
    abstract val baseline: RegularFileProperty

    @get:Internal
    abstract val projectDirPath: Property<String>

    private data class Rule(
        val label: String,
        val pathSegment: String,
        val forbiddenPrefixes: List<String>,
        val appliesToTests: Boolean = true,
    )

    private companion object {
        const val APP_PACKAGE = "restarhalf.stellar.schedule"

        val RULES = listOf(

            Rule(
                label = "domain 不得依赖 data/ui/platform",
                pathSegment = "/domain/",
                forbiddenPrefixes = listOf(
                    "$APP_PACKAGE.data.",
                    "$APP_PACKAGE.ui.",
                    "$APP_PACKAGE.platform.",
                ),
            ),
            Rule(
                label = "ui 不得依赖 data",
                pathSegment = "/ui/",
                forbiddenPrefixes = listOf("$APP_PACKAGE.data."),
            ),
            Rule(
                label = "core 不得依赖 domain/data/ui/platform",
                pathSegment = "/core/",
                forbiddenPrefixes = listOf(
                    "$APP_PACKAGE.domain.",
                    "$APP_PACKAGE.data.",
                    "$APP_PACKAGE.ui.",
                    "$APP_PACKAGE.platform.",
                ),
            ),

            Rule(
                label = "data 不得依赖 ui",
                pathSegment = "/data/",
                forbiddenPrefixes = listOf("$APP_PACKAGE.ui."),
            ),

            Rule(
                label = "表现层不得直连仓库接口（请经 UseCase / Port）",
                pathSegment = "/ui/viewmodel/",
                forbiddenPrefixes = listOf("$APP_PACKAGE.domain.repository."),
                appliesToTests = false,
            ),
            Rule(
                label = "表现层不得直连仓库接口（请经 UseCase / Port）",
                pathSegment = "/ui/screens/",
                forbiddenPrefixes = listOf("$APP_PACKAGE.domain.repository."),
                appliesToTests = false,
            ),

            Rule(
                label = "reminder 层不得依赖 data（请经 Port）",
                pathSegment = "/reminder/",
                forbiddenPrefixes = listOf("$APP_PACKAGE.data."),
            ),
            Rule(
                label = "widget 层不得依赖 data（请经 Port）",
                pathSegment = "/widget/",
                forbiddenPrefixes = listOf("$APP_PACKAGE.data."),
            ),
        )
    }

    @TaskAction
    fun verify() {
        val root = projectDirPath.get().replace('\\', '/')
        val allowed = baseline.get().asFile
            .readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toSet()

        val violations = mutableListOf<String>()

        fun scan(files: Collection<File>, isTest: Boolean) {
            files.sortedBy { it.path }.forEach fileLoop@{ file ->
                val relative = file.path.replace('\\', '/').removePrefix("$root/")
                val applicable = RULES.filter {
                    relative.contains(it.pathSegment) && (isTest.not() || it.appliesToTests)
                }
                if (applicable.isEmpty()) return@fileLoop

                file.readLines().forEach lineLoop@{ line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("import ")) return@lineLoop

                    val symbol = trimmed.removePrefix("import ").trim().substringBefore(" as ")
                    val hit = applicable.firstOrNull { rule ->
                        rule.forbiddenPrefixes.any { symbol.startsWith(it) }
                    } ?: return@lineLoop
                    if ("$relative|$symbol" !in allowed) {
                        violations += "$relative -> $symbol  [${hit.label}]"
                    }
                }
            }
        }

        scan(prodSources.files, isTest = false)
        scan(testSources.files, isTest = true)

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("分层越界 ${violations.size} 处（如为有意改动，请同步更新 architecture-baseline.txt）：")
                    violations.forEach { appendLine("  $it") }
                }
            )
        }
    }
}

val verifyLayering = tasks.register<VerifyLayeringTask>("verifyLayering") {
    group = "verification"
    description = "按规则表校验分层依赖（domain/ui/core/data/reminder/widget，含基线白名单；生产与测试源集都扫）"
    projectDirPath.set(projectDir.absolutePath)
    baseline.set(architectureBaselineFile)
    prodSources.from(
        layout.projectDirectory.dir("src/commonMain/kotlin").asFileTree.matching { include("**/*.kt") },
        layout.projectDirectory.dir("src/androidMain/kotlin").asFileTree.matching { include("**/*.kt") },
        layout.projectDirectory.dir("src/iosMain/kotlin").asFileTree.matching { include("**/*.kt") },
    )

    testSources.from(
        layout.projectDirectory.dir("src/commonTest/kotlin").asFileTree.matching { include("**/*.kt") },
        layout.projectDirectory.dir("src/androidDeviceTest/kotlin").asFileTree.matching { include("**/*.kt") },
    )
}

tasks.named("check") { dependsOn(verifyLayering) }

composeCompiler {
    stabilityConfigurationFiles.add(
        project.layout.projectDirectory.file("compose_compiler_config.conf")
    )

    if (providers.gradleProperty("composeCompilerMetrics").orNull == "true") {
        metricsDestination = layout.buildDirectory.dir("compose_compiler/metrics")
        reportsDestination = layout.buildDirectory.dir("compose_compiler/reports")
    }
}

val localProps = Properties().apply {
    runCatching { rootProject.file("local.properties").inputStream().use(::load) }
}

fun readSecret(name: String): String? =
    (localProps.getProperty(name) ?: System.getenv(name))
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

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
        val aesKeyValue = aesKey.get()
        require(aesKeyValue.toByteArray(Charsets.UTF_8).size == 16) {
            "AES_KEY must be exactly 16 bytes for AES-128"
        }
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

        val aesChars = aesKeyValue.toHexStringArray()
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

    aesKey.set(providers.provider { readSecret("AES_KEY") ?: error("AES_KEY missing in local.properties or environment") })
    signKey.set(providers.provider { readSecret("SIGN_KEY") ?: error("SIGN_KEY missing in local.properties or environment") })
    outputDir.set(generatedLocalSecretsDir)
}

tasks.matching { it.name.startsWith("compile") || it.name.startsWith("ksp") }.configureEach {
    dependsOn(generateLocalSecrets)
}

if (providers.gradleProperty("composeCompilerMetrics").orNull == "true") {
    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile::class.java).configureEach {
        compilerOptions.moduleName.set("composeApp")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        if (providers.gradleProperty("composeCompilerMetrics").orNull == "true") {
            val metricsDir = layout.buildDirectory.dir("compose_compiler/metrics").get().asFile.absolutePath
            val reportsDir = layout.buildDirectory.dir("compose_compiler/reports").get().asFile.absolutePath
            freeCompilerArgs.addAll(
                "-Pplugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$metricsDir",
                "-Pplugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$reportsDir",
            )
        }
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
                implementation(libs.mmkv.kmp)
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
