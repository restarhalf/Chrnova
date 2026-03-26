# 大民课程表

![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?style=flat-square&logo=kotlin&logoColor=white) ![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4?style=flat-square) ![Android](https://img.shields.io/badge/Android-11%2B-3DDC84?style=flat-square&logo=android&logoColor=white) ![iOS](https://img.shields.io/badge/iOS-16.0%2B-000000?style=flat-square&logo=apple&logoColor=white)
[![Gitee](https://img.shields.io/badge/dynamic/json?url=https://gitee.com/api/v5/repos/restarhalf/schedule/tags%3Fsort%3Dupdated%26direction%3Ddesc&query=%24%5B0%5D.name&label=Gitee&style=flat-square&logo=gitee)](https://gitee.com/restarhalf/schedule/tags)
[![GitHub](https://img.shields.io/github/v/tag/restarhalf/scheduleKMP?label=GitHub&sort=semver&style=flat-square&logo=github)](https://github.com/restarhalf/scheduleKMP/tags)
[![QQGroup](https://img.shields.io/badge/QQ%E7%BE%A4-1084761691-blue?style=flat-square&logo=qq&logoColor=white)](https://qm.qq.com/cgi-bin/qm/qr?k=r3OzOC1jNUiLGm_ppUC3a3q0Bb3HPGxl&jump_from=webapi&authKey=XbHnq3HpTWDKTys0xW0E2i5wroL/LwmkqRZR5d1guNPhQL4VUawULXpgYtlCOv93)

一个给同学用的课程表应用。

它把课表、成绩、考试安排和提醒放到同一个 App 里，尽量做到打开就能看、切学期方便、同步教务直接、界面也别太难看。

如果你只是想知道这应用能做什么、怎么下、适不适合你用，看前几节就够了。

## 这是个什么应用

- 适用于DLNU教务系统使用场景
- 支持 Android 和 iOS
- 不是学校官方应用
- 更偏向“日常真的会打开用”的课程表，而不只是一个查课工具

## 你可以用它做什么

- 看今天有什么课，几点开始，在哪上
- 按周看完整课表，快速切换当前周前后
- 从教务系统同步最新课表
- 查考试安排，包括时间、地点、座位号
- 查成绩和成绩详情
- 开启课程提醒、考试提醒
- 添加或编辑实验课、处理调课场景
- 自定义背景、主题和界面风格
- 在 Android 上使用桌面小组件

## 适合谁

- 想要一个更顺手课程表的民大学生
- 不想每次都去教务系统翻课表、成绩、考试信息的人
- 想把课程提醒和考试提醒放到手机里统一处理的人

## 下载与使用

### Android

下载Release的app-release.apk，安装到 Android 11 及以上的设备上即可。

### iOS

下载Release的app-release.ipa，需要自签安装到 iOS 16.0 及以上的设备上。

## 使用前你需要知道

- 课表同步、成绩、考试安排这类功能依赖学校教务系统
- 首次使用相关功能时，需要登录教务账号
- 提醒功能需要授予通知权限
- 某些时间段没有考试、没有成绩或教务接口暂时无数据时，页面为空是正常情况

## 支持平台

| 平台      | 说明                     |
|---------|------------------------|
| Android | Android 11 及以上         |
| iOS     | 当前工程目标版本为 iOS 16.0 及以上 |

## 功能亮点

### 首页

- 直接展示当天日期和今日课程
- 更适合“打开就看今天有什么”

### 课程表

- 按周查看
- 自动识别当前周
- 支持滑动切周
- 支持实验课补充和调课处理

### 考试与成绩

- 课表之外，考试和成绩也能在同一个应用里查
- 不需要来回切不同入口

### 提醒

- 课程提醒
- 考试提醒

### 个性化

- 主题模式
- 底栏样式
- 背景图片与透明度、模糊度调整

## 关于这个仓库

这个仓库不仅是下载入口，也是完整源码仓库。

如果你只是普通使用者，其实重点看这些就够了：

- 这应用适不适合你
- 去哪里下载
- 支持哪些功能
- 有没有已知限制

如果你是开发者，或者准备自己构建，可以继续往下看。

## 开发者说明

### 项目结构

```text
.
├─ composeApp/   # 共享业务逻辑、Compose UI、Android/iOS 平台实现
├─ androidApp/   # Android 壳工程
├─ iosApp/       # iOS Xcode 工程
├─ .github/      # GitHub Actions
└─ README.md
```

### 本地运行

- JDK 21
- Android Studio 或 IntelliJ IDEA
- Xcode

Android：

```bash
./gradlew :androidApp:installDebug
```

iOS：

1. 用 Xcode 打开 [iosApp/iosApp.xcodeproj](iosApp/iosApp.xcodeproj)
2. 选择 `iosApp` scheme
3. 运行到模拟器或真机

### 本地配置

项目会在编译前生成 `LocalSecrets.kt`，所以需要提供 `AES_KEY`。

```properties
# local.properties
AES_KEY=your_16_byte_key
```

如果需要本地打 Android 签名包，还可以配置：

```properties
KEYSTORE_PATH=...
KEYSTORE_PASS=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

### 版本号

版本配置在：

- [AppVersion.xcconfig](iosApp/Configuration/AppVersion.xcconfig)

### 自动构建

仓库内置 GitHub Actions 工作流：

- [build-all-platforms.yml](.github/workflows/build-all-platforms.yml)

会构建：

- Android APK
- iOS IPA

## 声明

- 本项目并非大连民族大学官方应用
- 教务接口、字段格式和可用性可能随学校系统调整而变化
- 如果你准备把它改成别的学校使用，通常需要先替换教务接口适配层

## 致谢

本项目使用了以下项目的代码或参考其实现：

- [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)
