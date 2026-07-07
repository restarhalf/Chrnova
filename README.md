<div align="center">

# Chrnova

**一款基于 Kotlin Multiplatform 的校园课程表应用,纯本地客户端**

[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose--Multiplatform-1.11.1-4285F4.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)

</div>

---

## 功能特性

### 课程管理

- **智能课表** — 按周查看完整课程安排，支持单双周、调课
- **今日概览** — 首页展示当天课程，一目了然
- **课程编辑** — 自定义添加实验课，灵活调整课表

### 考试与成绩

- **考试安排** — 查看考试时间、地点、座位号
- **成绩查询** — 学期成绩报告，绩点统计
- **体测成绩** — 体育系统体测数据同步

### 校园服务

- **教务系统** — 一键登录教务系统同步数据
- **体育系统** — 体测成绩、二维码展示
- **选修学分** — 选修课程学分统计

### 个性化

- **自定义背景** — 上传背景图片，调整透明度
- **课程颜色** — 为不同课程设置颜色标识
- **多校区支持** — 支持金石滩校区等多校区配置

### 其他功能

- **课表提醒** — 课程/考试提醒通知
- **日志系统** — 完整的应用日志记录
- **应用更新** — 自动检查并更新应用

---

## 技术架构

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                         │
│  Compose Multiplatform + Miuix Components           │
├─────────────────────────────────────────────────────┤
│                 Navigation Layer                    │
│              Navigation3 (Jetpack)                  │
├─────────────────────────────────────────────────────┤
│                 ViewModel Layer                     │
│        Koin DI + Lifecycle ViewModel                │
├─────────────────────────────────────────────────────┤
│                  Domain Layer                       │
│          UseCases + Repository Interfaces           │
├─────────────────────────────────────────────────────┤
│                   Data Layer                        │
│  Room Database + Ktor + Multiplatform Settings      │
├─────────────────────────────────────────────────────┤
│                  Platform Layer                     │
│         Android / iOS (expect/actual)               │
└─────────────────────────────────────────────────────┘
```

---

## 技术栈

| 类别           | 技术                                     |
|--------------|----------------------------------------|
| **语言**       | Kotlin 2.4.0                           |
| **UI框架**     | Compose Multiplatform 1.11.1           |
| **UI组件**     | Miuix UI 0.9.3                         |
| **导航**       | Navigation3 1.2.0-alpha05              |
| **网络**       | Ktor 3.5.1                             |
| **数据库**      | Room 3.0.0                             |
| **DI**       | Koin 4.2.2                             |
| **序列化**      | Kotlinx Serialization 1.11.0           |
| **异步**       | Kotlinx Coroutines 1.11.0              |
| **日期**       | Kotlinx DateTime 0.8.0                 |
| **图片**       | Coil 3.5.0                             |
| **加密**       | WhyCryptography 0.6.0                  |
| **二维码**      | QRose 1.1.2                            |
| **Markdown** | Multiplatform Markdown Renderer 0.43.0 |

---

## 平台支持

| 平台          | 最低版本                 | 状态  |
|-------------|----------------------|-----|
| **Android** | API 24 (Android 7.0) | 稳定  |
| **iOS**     | iOS 16+              | 实验性 |

---

## 项目结构

```
ChrnovaClient/
├── composeApp/                    # 共享UI层 (KMP)
│   └── src/
│       ├── commonMain/           # 公共代码
│       │   ├── kotlin/
│       │   │   └── restarhalf/stellar/schedule/
│       │   │       ├── core/     # 核心工具
│       │   │       ├── data/     # 数据层
│       │   │       ├── di/       # 依赖注入
│       │   │       ├── domain/   # 领域层
│       │   │       ├── platform/ # 平台抽象
│       │   │       └── ui/       # UI层
│       │   └── composeResources/ # 资源文件
│       ├── androidMain/          # Android平台代码
│       └── iosMain/              # iOS平台代码
├── androidApp/                   # Android应用壳
├── iosApp/                       # iOS应用壳
├── worker/                       # Cloudflare Worker (后端)
└── website/                      # 项目网站
```

---

## 快速开始

### 环境要求

- JDK 17+
- Android Studio
- Xcode 15+ (仅iOS开发)
- Node.js 18+ (仅Worker开发)

### 配置密钥

在项目根目录创建 `local.properties` 文件：

```properties
SIGN_KEY=your_sign_key
AES_KEY=your_aes_key_here
PAPERS_BASE_URL=https://your-api-url.com
```

### 运行应用

```bash
# Android
./gradlew :androidApp:assembleRelease

# iOS
# 使用 Xcode 打开 iosApp/iosApp.xcodeproj
```

---

## 后端服务

项目包含一个 Cloudflare Worker 用于试卷共享管理：

```bash
cd worker
npm install
npm run dev    # 本地开发
npm run deploy # 部署到 Cloudflare
```

---

## 开源协议

本项目采用 [GNU Affero General Public License v3.0](LICENSE) 开源协议。

```
Copyright (C) 2024 restarhalf

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

---

## 致谢

- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) — 跨平台UI框架
- [miuix](https://github.com/yukonga/miuix) — 小米风格UI组件库
- [Room3](https://developer.android.com/jetpack/androidx/releases/room3) — 本地数据库
- [Ktor](https://ktor.io/) — 跨平台HTTP客户端

---

<div align="center">

**[回到顶部](#chrnova)**

</div>
