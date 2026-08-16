# Zhihu++ 项目结构分析

> **状态**: 基于 `master` 分支（2026-08-16）当前代码快照分析。
> **重点**: 模块组织、依赖关系、源集结构和架构模式。

---

## 1. 项目概述

Zhihu++ 是一个隐私增强的知乎 Android 客户端，支持本地内容过滤、广告屏蔽和自定义阅读体验。采用 **Kotlin Multiplatform (KMP)** 架构：App 模块是薄薄的 Android 壳，几乎所有业务逻辑、UI 和数据层都在 `shared` 模块中。

- **版本**: 0.28（buildCode 740）
- **最低 SDK**: 27
- **目标 SDK**: 35
- **编译 SDK**: 37
- **语言**: Kotlin 2.4.0 + Jetpack Compose
- **许可证**: AGPL-3.0

---

## 2. 模块清单

| 模块 | 类型 | 根目录 | 描述 |
|------|------|--------|------|
| `:app` | Android Application | `app/` | 薄 Android 应用壳：Activity、Manifest、资源 |
| `:shared` | KMP Library (Android) | `shared/` | 主要业务逻辑、Compose UI、Viewmodel、数据层 |
| `:shared-local-db` | KMP Library (Android) | `shared-local-db/` | Room 数据库：内容过滤实体与 DAO |
| `:markdown-parser` | KMP Library (vendored) | `third_party/markdown/markdown-parser/` | Markdown 解析为 AST |
| `:markdown-renderer` | KMP Library (vendored) | `third_party/markdown/markdown-renderer/` | AST → Compose 渲染 |
| `:markdown-runtime` | KMP Library (vendored) | `third_party/markdown/markdown-runtime/` | Markdown 运行时指令管道 |
| `rs-zse-sign` | Standalone Rust crate | `rs-zse-sign/` | ZSE 签名算法的 Rust 实现（参考，非 Gradle 模块） |

> **注意**: `settings.gradle.kts` 中仅包含 6 个 Gradle 模块。`desktopApp`、`sentence_embeddings` 和 `aigc-vote-server` 已在 `6be7b52`（"evolve: prune features"）提交中移除，不再是当前代码库的一部分。`rs-zse-sign` 作为独立 Rust 项目保留在仓库中，但 ZSE 签名已在 `shared` Kotlin 代码中重新实现 (`ZseSigner.kt`)，Rust 实现目前未通过 Gradle 集成。

---

## 3. 依赖关系图

```
┌─────────────────────────────────────────────────────────────┐
│                        :app (Android App)                   │
│  namespace: com.github.zly2006.zhihu                        │
│  applicationId: com.github.zly2006.zhplus.lite              │
│                                                             │
│  ┌──→ :shared                                               │
│  ├──→ :markdown-parser                                      │
│  ├──→ :markdown-renderer                                    │
│  ├──→ io.github.zly2006:latex-renderer-android              │
│  ├──→ coil3 (compose, gif, network-ktor3)                   │
│  ├──→ me.saket.telephoto:zoomable-image-coil3               │
│  ├──→ com.materialkolor:material-kolor                      │
│  ├──→ com.mikepenz:aboutlibraries-compose-m3                │
│  └──→ (Compose 1.11.1, ktor 3.5.0, jsoup, zxing)            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      :shared (KMP Library)                  │
│  namespace: com.github.zly2006.zhihu.shared                 │
│  Targets: Android library                                   │
│                                                             │
│  ┌──→ :shared-local-db                                      │
│  ├──→ :markdown-parser                                      │
│  ├──→ :markdown-renderer                                    │
│  ├──→ io.github.zly2006:latex-renderer (common)             │
│  ├──→ coil3 (compose, network-core)                         │
│  ├──→ com.materialkolor:material-kolor                      │
│  ├──→ com.fleeksoft.ksoup:ksoup                             │
│  ├──→ com.mikepenz:aboutlibraries-compose-m3                │
│  └──→ (Compose, ktor, kotlinx-serialization, datetime)      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 :shared-local-db (KMP Library)              │
│  namespace: com.github.zly2006.zhihu.shared.localdb         │
│  Targets: Android library                                   │
│                                                             │
│  ┌──→ androidx.room:room-runtime                            │
│  └──→ kotlinx-serialization-json                            │
│  (KSP: room-compiler → schema + DAO)                        │ 
└─────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                   Vendored Markdown (third_party)            │
│                                                              │
│  :markdown-parser    :markdown-runtime    :markdown-renderer │
│  pure parsing AST    runtime directives    render AST→Compose│
│  ────────────────→   ────────────────→   ────────────────→   │
│  (commonMain)        (commonMain)        (commonMain + jvm)  │
│       │                  │                  │                │
│       └──────────────────┴──────────────────┘                │
│                    depends chain                             │
│                                                              │
│  Targets: android, jvm, iosArm64, iosSimulatorArm64          │
└──────────────────────────────────────────────────────────────┘
```

### 依赖传递关系

- `markdown-renderer` → `markdown-runtime` → `markdown-parser`
- `shared` → `shared-local-db` (api dependency — DAOs/entities 对上层可见)
- `shared` → `markdown-parser`, `markdown-renderer`
- `app` → `shared`, `markdown-parser` (直接依赖), `markdown-renderer` (直接依赖)

---

## 4. 模块详述

### 4.1 `:app` — Android 应用壳

**源文件**: 9 个 Kotlin 文件 + 21 资源文件

```
app/
├── build.gradle.kts              # 应用配置 (debug/release)
├── proguard-rules.pro            # ProGuard 规则
├── aboutlibraries/               # 开源许可依赖配置
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/com/github/zly2006/zhihu/
    │   │   ├── MainActivity.kt           # 主 Activity：导航、生命周期、账号初始化
    │   │   ├── LoginActivity.kt          # 登录页面
    │   │   ├── QRCodeScanActivity.kt     # 二维码扫描
    │   │   ├── CaptureActivity.kt        # ZXing 扫码捕获
    │   │   ├── VideoPlayerActivity.kt    # 视频播放器
    │   │   ├── WebviewActivity.kt        # WebView 页面
    │   │   └── PhoneLoginPane.kt         # 手机号登录 UI
    │   ├── java/com/github/zly2006/zhihu/ui/
    │   │   ├── ZhihuMainAndroidContent.kt  # shared ZhihuMain 的 Android 适配
    │   │   └── ZhihuMainAndroidState.kt    # Android 专用状态
    │   ├── res/                        # 图标、主题、字符串（en/zh）
    │   └── assets/                     # HTML 模板、JS、CSS
    ├── androidTest/                  # Instrumented tests
    └── test/                         # Unit tests
```

**构建变体**:
- `applicationId`: `com.github.zly2006.zhplus.lite` (固定，无 flavor 后缀)
- `debug`/`release`: release 启用 minify + shrinkResources

**关键职责**:
- `MainActivity.kt` (644 行)：应用入口，负责导航控制、账号初始化 (`AccountData.loadData`)、内容过滤数据库初始化、Emoji 初始化、阅读播放器桥接、deeplink/剪贴板解析、错误处理对话框。
- `ZhihuMainAndroidContent.kt`: 将 `MainActivity` 的状态注入到共享 `ZhihuMain` Composable，实现平台适配（文章页 ViewModel、转场动画、TTS 状态）。

### 4.2 `:shared` — KMP 核心库

**源文件**: ~270 Kotlin 文件（commonMain 160 + androidMain 29 + commonTest 68 + jvmTest 13）

```
shared/src/
├── commonMain/kotlin/com/github/zly2006/zhihu/
│   ├── account/      # 账号认证：登录、二维码登录、电话登录、身份管理
│   ├── data/         # 数据模型：Feed、Article、DataHolder、搜索、通知、每日故事
│   ├── editor/       # 内容编辑器：图片上传、Markdown 编译、发布支持
│   ├── filter/       # 内容过滤：过滤规则、内容打开事件追踪
│   ├── markdown/     # Markdown 渲染：AST、运行时、指令渲染
│   ├── navigation/   # 导航：NavDestination 密封类、URL 解析、路由
│   ├── notification/ # 通知：在线消息、通知类型、通知设置
│   ├── platform/     # 平台抽象：PlatformCapabilities (expect/actual)
│   ├── reading/      # 阅读播放器：文本转语音、阅读队列、播放状态
│   ├── theme/        # 主题：颜色、排版、主题管理（亮/暗/跟随系统）
│   ├── ui/           # Compose 界面：主屏、文章页、评论、设置等
│   │   ├── article/  # 文章页组件
│   │   ├── components/  # 可复用 UI 组件
│   │   └── subscreens/  # 设置子页面
│   ├── util/         # 工具：ZSE 签名、凭据刷新、URL 解析、Emoji
│   └── viewmodel/    # ViewModel 层：分页、Feed、评论、问题、过滤
│       ├── comment/  # 评论 ViewModel
│       ├── feed/     # Feed 流 ViewModel
│       ├── filter/   # 过滤 ViewModel
│       └── za/       # 问答（zhihu-answers）ViewModel
├── androidMain/kotlin/com/github/zly2006/zhihu/
│   ├── data/AccountData.kt      # Android 账号数据客户端配置
│   ├── data/AndroidDataSupport.kt
│   ├── data/HistoryStorage.kt
│   ├── platform/AndroidPlatformCapabilities.kt
│   ├── reading/AndroidReadingPlayer.kt
│   ├── reading/ContentReadingService.kt
│   ├── theme/AndroidThemeSupport.kt
│   ├── ui/AndroidUiRuntimes.kt
│   ├── ui/components/AndroidComponents.kt
│   ├── ui/subscreens/AndroidSubscreenRuntimes.kt
│   ├── util/Utils.kt, Log.android.kt, EmojiManager.kt, etc.
│   ├── viewmodel/AndroidPaginationEnvironment.android.kt
│   └── viewmodel/filter/AndroidFilterSupport.kt
├── commonTest/kotlin/            # 68 个测试文件
└── jvmTest/kotlin/               # 13 个 JVM 测试 + 资源
```

**注意**: `shared/src/main/` 目录存在但为空（无 Kotlin 文件），是构建系统的遗留产物。

**架构模式**:
- **Expect/Actual**: `PlatformCapabilities.kt` (common) ↔ `AndroidPlatformCapabilities.kt` (Android)
- **StateFlow + ViewModel**: `PaginationViewModel` 作为分页列表的基类
- **Sealed interface routes**: `NavDestination.kt` 使用 `@Serializable` 实现类型安全导航

**关键文件**:
| 文件 | 行数 | 作用 |
|------|------|------|
| `ZhihuMain.kt` | 819 | 主 UI 外壳：底栏、主 Pager、NavHost |
| `DataHolder.kt` | 970 | 数据模型持有者：Kotlinx.serialization 自动转换 |
| `ZhihuDataCore.kt` | 152 | 数据核心：Feed/文章/答案获取入口 |
| `ZseSigner.kt` | 168 | ZSE 签名算法纯 Kotlin 实现 |
| `ZhihuMain.kt` | 819 | 主界面 Composable + 导航逻辑 |
| `AccountData.kt` (androidMain) | ~500 | Android 账号客户端、HTTP 客户端、设置 |

### 4.3 `:shared-local-db` — Room 数据库

**源文件**: 18 个 Kotlin 文件

```
shared-local-db/src/
├── commonMain/kotlin/com/github/zly2006/zhihu/
│   ├── data/RoomDriverConfig.kt                 # Room 驱动配置 (expect)
│   └── viewmodel/filter/
│       ├── BlockedFeedRecord.{kt,Dao.kt}       # 屏蔽的 Feed 记录
│       ├── BlockedKeyword.{kt,Dao.kt}          # 屏蔽的关键词
│       ├── BlockedQuestionAuthor.{kt,Dao.kt}   # 屏蔽的问题作者
│       ├── BlockedTopic.{kt,Dao.kt}            # 屏蔽的话题
│       ├── BlockedUser.{kt,Dao.kt}             # 屏蔽的用户
│       ├── ContentViewRecord.kt                # 内容浏览记录
│       ├── ContentOpenEvent.{kt,Dao.kt}        # 内容打开事件
│       ├── ContentFilterDao.kt                 # 内容过滤 DAO
│       └── ContentFilterDatabase.kt           # Room 数据库定义
└── androidMain/kotlin/com/github/zly2006/zhihu/
    ├── data/RoomDriverConfig.android.kt        # Android Room 驱动实现
    └── viewmodel/filter/AndroidContentFilterDatabase.kt
```

**功能**:
- `ContentFilterDatabase`: Room 数据库，存储内容过滤数据
- `ContentFilterDao`: 提供查询/插入/删除屏蔽记录、内容打开事件等
- 仅 Android 平台支持（`commonMain` 定义接口，`androidMain` 实现 Room 驱动）

### 4.4 Vendored Markdown 库 (`third_party/markdown/`)

三个独立的 KMP 库，由 Huarangmeng (`huarangmeng`) 的 `latex` fork 维护：

| 模块 | namespace | 作用 | 目标 |
|------|-----------|------|------|
| `markdown-parser` | `com.hrm.markdown.parser` | 解析 Markdown → AST | android, jvm, iosArm64, iosSimulatorArm64 |
| `markdown-runtime` | `com.hrm.markdown.runtime` | 运行时指令管道 | android, jvm, iosArm64, iosSimulatorArm64 |
| `markdown-renderer` | `com.hrm.markdown.renderer` | AST → Compose UI 渲染 | android, jvm, iosArm64, iosSimulatorArm64 |

**依赖链**: `markdown-renderer` → `markdown-runtime` → `markdown-parser`

---

## 5. 构建系统

### 5.1 根 Build 配置

- **Kotlin**: 2.4.0
- **Android Gradle Plugin**: 9.3.1
- **Compose Multiplatform**: 1.11.1
- **KSP**: 2.3.9 (用于 Room 代码生成)
- **ktlint**: 14.2.0

### 5.2 版本约束

项目强制约束 `material3:1.10.0-alpha05` 用于解决 `shared` 模块中 `material-kolor` 的版本冲突问题。

### 5.3 Build 类型

| 类型 | minify | shrinkResources | 说明 |
|------|--------|-----------------|------|
| debug | false | false | 调试用，包含 BuildConfig |
| release | true | true | 启用 ProGuard + 资源压缩 |

> **注意**: `app/build.gradle.kts` 中已移除 `productFlavors` 配置。`applicationId` 直接设为
> `com.github.zly2006.zhplus.lite`，不再通过 flavor suffix 区分。`androidComponents` 中原有的
> flavor-specific minify/shrink 逻辑也已移除，`buildTypes.release` 的 `minifyEnabled=true`／
> `shrinkResources=true` 直接作用于唯一 variant。

### 5.4 其他配置文件

| 文件/目录 | 说明 |
|-----------|------|
| `gradle.properties` | 版本 0.28 (code 740)，JVM 工具链 17，启用 R8 full mode、构建缓存、配置缓存 |
| `buildSrc/src/main/kotlin/buildlogic/git.kt` | Git 哈希/分支获取工具 (用于 BuildConfig.GIT_HASH) |
| `.claude/settings.json` | Claude Code 工具权限配置 |
| `.github/` | GitHub Actions 工作流、Issue 模板 |

---

## 6. 架构模式

### 6.1 薄 App + 厚 Shared

```
app/ (Android壳 — ~10 文件)
  └── MainActivity → ZhihuMainAndroidContent → shared.ZhihuMain
                                ↑
                                │ commonMain
shared/ (核心逻辑 — ~250 文件)
  ├── commonMain: 所有业务逻辑 + Compose UI
  ├── androidMain: Android 专用实现 (AccountData, 平台能力, 工具)
  └── tests: commonTest + jvmTest
```

App 模块仅负责：
- Android Activity 生命周期
- 账号初始化 (`AccountData.loadData`)
- 内容过滤数据库初始化
- deeplink / 剪贴板解析
- 导航控制器管理
- 平台依赖注入（图片加载器、主题）

### 6.2 KMP Expect/Actual

```
commonMain/platform/PlatformCapabilities.kt   → expect
androidMain/platform/AndroidPlatformCapabilities.kt  → actual
```

### 6.3 类型安全导航

`NavDestination.kt` 使用 `@Serializable sealed interface` 实现类型安全导航：
- `MainTabs` (主壳)
- `Home`, `Follow`, `HotList`, `Daily`, `OnlineHistory`, `MyCollections` (顶层目标)
- `Article`, `Question`, `Pin`, `Topic`, `Person`, `Video` (内容页)
- `Notification.*`, `Account.*`, `Search` 等嵌套目标

`resolveContent(url: Url)` 实现统一的 URL → NavDestination 解析。

### 6.4 数据层

```
DataHolder.kt (970行) — 核心数据模型
  ├── 使用 @Serializable + 自定义 KSerializer
  ├── snake_case ↔ camelCase 自动转换
  └── ZhihuJson (单例 Json 配置)

ZhihuDataCore.kt — 数据获取入口
ZhihuApiClients.kt — HTTP 客户端 + ZSE 签名
```

### 6.5 ContentFilterDatabase (Room)

```
ContentFilterDatabase (Room)
├── BlockedUser / BlockedTopic / BlockedKeyword
├── BlockedFeedRecord / BlockedQuestionAuthor
├── ContentOpenEvent / ContentViewRecord
└── ContentFilterDao
```

---

## 7. 近期剪裁内容

来自 `6be7b52` + `PLAN.md`:
- ✅ Desktop 平台 (desktopApp/)
- ✅ iOS/JVM 平台支持 (sourceSets)
- ✅ NLP/AI 功能 (HanLP, sentence_embeddings)
- ✅ AIGC 投票服务器 (aigc-vote-server/)
- ✅ GitHub 更新检查 (updater/)
- ✅ 遥测统计 (ContinuousUsageReminder)
- ✅ 防沉迷功能 (FuckHonorService, PowerSaveModeCompat)
- ✅ 开发者选项 (DeveloperSettingsScreen)
- ✅ 反向屏蔽 (reverseBlock)

---

## 8. 外部依赖项概览

| 类别 | 依赖 | 版本 |
|------|------|------|
| Compose | runtime, foundation, material3, ui, animation | 1.11.1 |
| Compose Material3 | material3 (forced) | 1.10.0-alpha05 |
| Ktor | client-core, client-android, content-negotiation, serialization | 3.5.0 |
| Coil | coil-compose, coil-network-ktor3-android, coil-gif | 3.5.0 |
| Kotlinx | coroutines-core, datetime, serialization-json, io-core | 1.11.0 / 0.8.0 / 1.11.0 / 0.8.1 |
| Material Kolor | material-kolor | 4.1.1 |
| ksoup | ksoup (Kotlin HTML parser) | 0.2.6 |
| LaTeX | latex-renderer (android/common), latex-base, latex-parser | 0.0.1-alpha5 |
| ZXing | zxing-android-embedded | 4.3.0 |
| Telephoto | zoomable-image-coil3 | 0.19.0 |
| About Libraries | aboutlibraries-compose-m3 | 15.0.0 |
| Lifecycle | lifecycle-runtime-compose, lifecycle-viewmodel-compose (JetBrains) | 2.10.0 |
| Navigation | navigation-compose | 2.9.2 |
| Room | room-runtime (KSP: room-compiler) | 2.8.4 |
| jsoup | jsoup (HTML parsing) | 1.22.2 |

---

## 9. 目录树概览

```
zhihu-plus-plus/
├── app/                          # Android 应用 (thin wrapper)
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   ├── aboutlibraries/
│   └── src/
│       ├── main/                 # 9 Kotlin files + 资源 + assets
│       ├── androidTest/          # Instrumented tests
│       └── test/                 # Unit tests
├── shared/                       # KMP 核心库 (~250 Kotlin files)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/           # 160 Kotlin files — 业务逻辑 + UI
│       ├── androidMain/          # 29 Kotlin files — Android 实现
│       ├── commonTest/           # 68 test files
│       ├── jvmTest/              # 13 JVM test files + 资源
│       └── main/                 # (empty - leftover)
├── shared-local-db/              # KMP Room 数据库 (18 files)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/           # 实体 + DAO (common)
│       ├── androidMain/          # Room 驱动 (Android)
│       └── main/                 # (empty - leftover)
├── third_party/markdown/         # Vendored markdown 库 (3 modules)
│   ├── markdown-parser/          # 解析 AST
│   ├── markdown-runtime/         # 运行时指令
│   └── markdown-renderer/        # AST → Compose
├── rs-zse-sign/                  # Rust ZSE 签名 (standalone, reference)
│   ├── Cargo.toml
│   ├── src/lib.rs, decrypt.rs, main.rs
│   └── Cargo.lock
├── buildSrc/                     # 构建逻辑
│   └── src/main/kotlin/buildlogic/git.kt
├── docs/                         # 设计文档
│   ├── ai-ui-design-guide.md     # UI 设计指南
│   ├── notification-center.md    # 通知中心复刻说明
│   └── project-structure-analysis.md  # 本文档
├── fastlane/                     # Play Store 发布
│   └── metadata/android/zh-CN/
├── misc/                         # 工具脚本
│   ├── chrome-zhihu-ad-filter/   # Chrome 广告过滤扩展
│   ├── emoji.py + emoji_mapping.json + emojis/
│   ├── build-dmg.sh, repack_release_apk.py
│   ├── install-avd-system-cert.py
│   ├── htk-inject-system-cert.sh
│   ├── tampermonkey.js
│   └── zse-ck-v4-*.js            # ZSE 检查脚本
├── .agents/skills/               # Claude Code 技能
├── .claude/
│   └── settings.json             # 工具权限配置
├── PLAN.md                       # 裁剪计划 (untracked)
├── build.gradle.kts              # 根构建
├── settings.gradle.kts           # 6 个模块
└── gradle.properties
```

---

## 10. 关键约定

1. **DataHolder / data class**: 使用 `camelCase` 命名，Zhihu API 返回 `snake_case`，`AccountData.fetch*()` 和 `decodeJson()` 自动转换
2. **HTTP 客户端**: `AccountData.httpClient(context)` 获取；Web API 需要 `signFetchRequest(context)` 签名
3. **Compose**: Material 3，`LaunchedEffect` 处理副作用，使用正确的 key
4. **导航**: Jetpack Navigation Compose，sealed interface `NavDestination`
5. **ZSE 签名**: 纯 Kotlin 实现在 `shared` 模块 (`ZseSigner.kt`)，Rust 版本 (`rs-zse-sign`) 作为参考
6. **Room**: `shared-local-db` 模块通过 KSP 生成 schema，存储在 `schemas/` 目录
7. **WebView**: 正文渲染只支持 Compose Markdown，WebView 作为废弃路径保留
8. **构建**: `assembleDebug` 构建调试 APK，`assembleRelease` 构建发布 APK，`ktlintFormat` 格式化
