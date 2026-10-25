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

// 分层守卫：按规则表校验「某层源文件不得 import 某些前缀」。
// 已知越界记录在 architecture-baseline.txt（每行 <相对路径>|<import 符号>），逐步清空后即零容忍。
// 生产源集与测试源集都扫：测试目录镜像主源码，同一套规则同样适用；
// 测试里实现真实端口（fake/stub）不算越界，越界的只是「测试去 import data 实现类」这类硬依赖。
val architectureBaselineFile = layout.projectDirectory.file("architecture-baseline.txt")

abstract class VerifyLayeringTask : DefaultTask() {
    /** 生产源集：全部规则适用。 */
    @get:InputFiles
    abstract val prodSources: ConfigurableFileCollection

    /** 测试源集：只套用「层归属」规则，不套用「表现层不得直连仓库」规则。 */
    @get:InputFiles
    abstract val testSources: ConfigurableFileCollection

    @get:InputFile
    abstract val baseline: RegularFileProperty

    @get:Internal
    abstract val projectDirPath: Property<String>

    /**
     * 一条规则：路径含 [pathSegment] 的文件，不得 import [forbiddenPrefixes] 开头的符号。
     *
     * [appliesToTests] 为 false 时只约束生产代码。测试里 mock 一个仓库接口、再包进
     * UseCase 去构造被测对象是标准做法，不属于「表现层绕过用例层」。
     */
    private data class Rule(
        val label: String,
        val pathSegment: String,
        val forbiddenPrefixes: List<String>,
        val appliesToTests: Boolean = true,
    )

    private companion object {
        const val APP_PACKAGE = "restarhalf.stellar.schedule"

        val RULES = listOf(
            // 核心分层
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
            // 反向依赖：数据层不得回头依赖表现层
            Rule(
                label = "data 不得依赖 ui",
                pathSegment = "/data/",
                forbiddenPrefixes = listOf("$APP_PACKAGE.ui."),
            ),
            // 表现层不得绕过用例层直连仓库
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
            // 平台胶水层（不在 domain/ui 命名空间下，原先被规则漏掉）
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
                    // 去掉 `as Alias`，让基线条目稳定且可读
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
    // 测试目录镜像主源码，层归属规则同样适用：测试直接 import data 实现类
    // 同样是硬依赖，会让重构数据层时连带改测试。
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
    // 需要重组分析时：./gradlew :composeApp:compileAndroidMain -PcomposeCompilerMetrics=true
    // 产物：composeApp/build/compose_compiler/{metrics,reports}
    if (providers.gradleProperty("composeCompilerMetrics").orNull == "true") {
        metricsDestination = layout.buildDirectory.dir("compose_compiler/metrics")
        reportsDestination = layout.buildDirectory.dir("compose_compiler/reports")
    }
}

val localProps = Properties().apply {
    runCatching { rootProject.file("local.properties").inputStream().use(::load) }
}

// 密钥在任务执行时才解析：配置期不再 error，这样只跑校验类任务
// （如 verifyLayering）的人无需准备 local.properties。
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
    // 惰性求值：只有真正执行到该任务时才要求密钥存在。
    aesKey.set(providers.provider { readSecret("AES_KEY") ?: error("AES_KEY missing in local.properties or environment") })
    signKey.set(providers.provider { readSecret("SIGN_KEY") ?: error("SIGN_KEY missing in local.properties or environment") })
    outputDir.set(generatedLocalSecretsDir)
}

tasks.matching { it.name.startsWith("compile") || it.name.startsWith("ksp") }.configureEach {
    dependsOn(generateLocalSecrets)
}

// Compose Compiler Metrics 在 Windows 上不能写含 ':' 的文件名；
// KMP 默认 -module-name 为 "根项目:模块"（Chrnova:composeApp），会导致 report 变成 0 字节。
// 仅在收集 metrics 时改成不含冒号的名字。
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