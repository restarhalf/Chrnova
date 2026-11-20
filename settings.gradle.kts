rootProject.name = "Chrnova"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenLocal()
        mavenCentral()
        // 柔光玻璃库（独立仓库，tag 触发 JitPack 构建）
        maven("https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\.HaoZai000.*") }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// 柔光玻璃是独立仓库（HaoZai000/softglass）。本地存在源码时用复合构建直接编源码，
// 改库即改即测；没有源码（CI / 他人 clone）时才回落到 JitPack 坐标。
// 注意：JitPack 只发布 android/jvm，iOS 目标必须用本地源码复合构建。
// 复合构建按 group:name 精确替换，而 JitPack 给的是 com.github.HaoZai000 前缀，
// 与库自身的 group 不同，所以必须显式声明 substitution。
val softglassSource = listOf(file("softglass"), file("../softglass"))
    .firstOrNull { it.resolve("build.gradle.kts").isFile }
if (softglassSource != null) {
    includeBuild(softglassSource) {
        dependencySubstitution {
            substitute(module("com.github.HaoZai000.softglass:softglass")).using(project(":"))
        }
    }
}

include(":composeApp")
include(":androidApp")
include(":baselineprofile")
include(":miuix")
include(":ui-components")
include(":picture-selector")
