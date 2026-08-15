# Zhihu++ 架构图

> **项目概述**：Zhihu++ 是一个基于 Kotlin Multiplatform (KMP) 的隐私增强知乎客户端，
> 支持 Android、Desktop (JVM) 以及规划中的 iOS 平台。
> 采用共享业务逻辑 + 平台专用 UI 壳的架构模式。

---

## 1. 模块依赖图 (Gradle Module)

```
┌─────────────────────────────────────────────────────────┐
│                     :app (Android)                      │
│     Full flavor      ┌──────────────────┐               │
│   (NLP + HanLP)      │  :sentence_emb   │               │
│                      └──────────────────┘               │
│                      ┌──────────────────┐               │
│                      │  :shared-local-db │               │
│                      └──────────────────┘               │
│  ┌─────────────────────────────────────┐               │
│  │        :shared (KMP common)         │ ◄─────────────┘
│  │                                     │
│  │  ┌──────────┐ ┌──────────┐         │
│  │  │ markdown │ │ latex    │         │
│  │  │-parser    │ │-renderer│         │
│  │  │-renderer   │ └──────────┘         │
│  │  │-runtime    │                      │
│  │  └──────────┘                      │
│  └─────────────────────────────────────┘               │
│                                                         │
│  ┌─────────────────────────────────────┐               │
│  │     :third_party/markdown/*         │ (vendored)   │
│  └─────────────────────────────────────┘               │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                   :desktopApp (JVM)                      │
│                    (Compose Desktop)                     │
│  ┌────────────────────────┐                              │
│  │       :shared          │ ◄───────────────────────────┘
│  └────────────────────────┘
│  ┌────────────────────────┐
│  │ :shared-local-db        │
│  └────────────────────────┘
│  ┌────────────────────────┐
│  │ JavaFX (WebView for     │
│  │ risk-control pages)     │
│  └────────────────────────┘
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                 :aigc-vote-server (Rust)                 │
│  (Self-hosted AIGC 标记投票后端)                          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│              :rs-hf-tokenizer / :rs-zse-sign             │
│       (Native Rust libraries for NLP/Signing)            │
└─────────────────────────────────────────────────────────┘
```

### 依赖方向

```
app ──────► shared ──────► shared-local-db
desktopApp ─► shared ─────► shared-local-db
                  │
                  ├─────► third_party/markdown/*
                  │
                  └─────► (io.ktor, coil3, compose,
                           room, latex-renderer,
                           material-kolor)
```

### shared 模块 – KMP 源集合结构

```
shared/src/
├── commonMain/          ← 所有业务逻辑、UI、ViewModel 的共享代码
│   ├── account/         ← 账号登录、会话管理、身份认证
│   ├── data/            ← 数据模型 (Feed, DataHolder, ZhihuApiClients)
│   ├── navigation/      ← NavDestination 密封类体系
│   ├── ui/              ← Compose UI 屏幕 + 组件
│   │   ├── components/  ← 可复用 Compose 组件
│   │   └── subscreens/  ← 设置子页面
│   ├── viewmodel/       ← ViewModel 基类 + 环境接口
│   │   ├── feed/        ← Feed 相关 ViewModel
│   │   ├── comment/     ← 评论 ViewModel
│   │   ├── local/       ← 本地推荐引擎
│   │   ├── filter/      ← 内容过滤支持
│   │   └── za/          ← 混合/安卓专用 Feed ViewModel
│   ├── markdown/        ← Markdown 渲染 (common 声明)
│   ├── filter/          ← 内容曝光记录、过滤统计
│   ├── notification/    ← 在线通知
│   ├── reading/         ← 阅读播放器 (TTS)
│   ├── theme/           ← 主题管理
│   ├── editor/          ← 内容发布编辑器
│   ├── nlp/             ← NLP 抽词接口 (common)
│   └── util/            ← 工具函数
├── androidMain/         ← Android 专用实现 (HttpClient, AccountData, 等)
├── jvmMain/             ← Desktop/JVM 专用实现 (DesktopZhihuMain, DesktopInfra)
├── iosMain/             ← iOS 专用实现 (存根)
├── nativeMain/          ← Native 专用实现 (存根)
├── commonTest/          ← 共享测试
└── jvmTest/             ← JVM 测试
```

---

## 2. 运行时架构层次图

```
┌─────────────────────────────────────────────────────────┐
│                   ┌─── MainActivity (Android)           │
│  ┌─────────────┐  │  ┌─── VideoPlayerActivity            │
│  │  Platform   │  │  ├── QRCodeScanActivity              │
│  │  Entry      │  │  ├── LoginActivity                    │
│  │  Point      │  │  └── WebviewActivity                   │
│  └──────┬──────┘  └──┼────────────────────────────────────┘
│         │              │
│         │          ┌───▼─── Desktop entry point (JVM)
│         │          │   ┌─── Main.kt (Compose Desktop Window)
│         │          │   └── DesktopZhihuMain()
│         │          └──┌─── iOS entry (规划中)
│         │
│         ▼
│  ┌─────────────────────────────────────────────────────────┐
│  │  ┌──────────────────┐  ┌──────────────────┐             │
│  │  │ AndroidZhihuMain │  │ DesktopZhihuMain │             │
│  │  │  (androidMain)  │  │    (jvmMain)     │             │
│  │  └────────┬─────────┘  └────────┬─────────┘             │
│  │           │  (platform glue)     │                        │
│  │           └─────────┬────────────┘                        │
│  │                     │                                      │
│  │                     ▼                                      │
│  │  ┌─────────────────────────────────────────────────────┐ │
│  │  │                    ZhihuMain                         │ │
│  │  │          (commonMain/ui/ZhihuMain.kt)                │ │
│  │  │  ─ 主导航壳：底部栏 + 横向 pager + NavHost ─        │ │
│  └────────────────────────────────────────────────────────┘ │
│                     │                                          │
│         ┌───────────┼───────────┐                              │
│         ▼           ▼           ▼                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                        │
│  │  主 Tab  │ │  详情页  │ │  设置页  │                        │
│  │ 页面组合 │ │  (各种  │ │  (子屏   │                        │
│  │          │ │  NavDes- │ │  等)     │                        │
│  │          │ │  tination)│ │         │                        │
│  └──────────┘ └──────────┘ └──────────┘                        │
│                                                                 │
│  └─────────────────────────────────────────────────────────────┘
│         │
│         ▼
│  ┌─────────────────────────────────────────────────────────┐
│  │          Compose UI + ViewModel 层 (common)            │
│  │  ─ HomeScreen, ArticleScreen, QuestionScreen, ...    │
│  │                                                    │
│  │  ┌────────────────────┐ ┌────────────────────┐     │
│  │  │ PaginationView-    │ │ ArticleViewModel    │     │
│  │  │ Model (base)        │ │ (content detail)    │     │
│  │  └────────┬───────────┘ └────────┬────────────┘     │
│  │           │                       │                │
│  │  ┌────────┴────────┐  ┌──────────┴───────────┐    │
│  │  │ Feed ViewModels  │  │ Comment ViewModels   │    │
│  │  │ HomeFeedVM       │  │ RootCommentVM        │    │
│  │  │ FollowVM         │  │ ChildCommentVM        │    │
│  │  │ HotListVM        │  └───────────────────────┘    │
│  │  │ DailyVM          │                               │
│  │  │ SearchVM         │  ┌───────────────────────┐    │
│  │  └───────────────────┘  │ LocalRecommendation   │    │
│  │                        │ Engine                 │    │
│  │                        └───────────────────────┘    │
│  │                                                     │
│  │  ┌──────────────────────┐                          │
│  │  │ ContentFilterManager │  ◄── Room DAO            │
│  │  └──────────────────────┘                           │
│  └─────────────────────────────────────────────────────┘
│         │
│         ▼
│  ┌─────────────────────────────────────────────────────────┐
│  │  Environment 抽象层 (commonMain/viewmodel/*.kt)         │
│  │  ─ ZhihuApiEnvironment, PaginationEnvironment,         │
│  │    AccountEnvironment, ArticleExportEnvironment, ...    │
│  │  ─── Platform 实现通过 expect/actual 提供              │
│  └─────────────────────────────────────────────────────────┘
│         │
│         ▼
│  ┌─────────────────────────────────────────────────────────┐
│  │  Data Layer                                              │
│  │  ┌──────────────────────────────────────────────┐      │
│  │  │  Network: Ktor HTTP Client                   │      │
│  │  │  ─ executeZhihuAuthenticatedRequest()         │      │
│  │  │  ─ signZhihuFetchRequest() (zse96 v2)         │      │
│  │  │  ─ ContentDetailCache (in-memory LRU, 10min)  │      │
│  │  └──────────────────────────────────────────────┘      │
│  │                                                      │
│  │  ┌──────────────────────────────────────────────┐      │
│  │  │  Local DB: Room (shared-local-db module)     │      │
│  │  │  ─ ContentFilterDatabase                     │      │
│  │  │    • BlockedUser DAO                         │      │
│  │  │    • BlockedTopic DAO                        │      │
│  │  │    • BlockedKeyword DAO                      │      │
│  │  │    • BlockedQuestionAuthor DAO               │      │
│  │  │    • ContentViewRecord DAO                   │      │
│  │  │    • ContentOpenEvent DAO                    │      │
│  │  │    • BlockedContentRecord DAO               │      │
│  │  │  ─ LocalContentDatabase                     │      │
│  │  │    • CrawlingTask DAO                        │      │
│  │  │    • CrawlingResult DAO                      │      │
│  │  │    • LocalFeed DAO                           │      │
│  │  │    • UserBehavior DAO                        │      │
│  │  └──────────────────────────────────────────────┘      │
│  └─────────────────────────────────────────────────────────┘
│         │
│         ▼
│  ┌─────────────────────────────────────────────────────────┐
│  │  External Services                                       │
│  │  ┌────────────────┐  ┌────────────────┐  ┌───────────┐ │
│  │  │  Zhihu API    │  │ AIGC Vote      │  │ GitHub    │ │
│  │  │  (REST)      │  │ Server (Rust)  │  │ Releases  │ │
│  │  └────────────────┘  └────────────────┘  └───────────┘ │
│  └─────────────────────────────────────────────────────────┘
│         │
│         ▼
│  ┌─────────────────────────────────────────────────────────┐
│  │  Third-Party Libraries                                   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐   │
│  │  │ markdown-    │ │ latex-       │ │ Coil3        │   │
│  │  │ parser/      │ │ renderer     │ │ (image)      │   │
│  │  │ renderer/    │ │ (fork)       │ │              │   │
│  │  │ runtime       │ │              │ │ HanLP (Full) │   │
│  │  │ (fork)       │ │              │ │              │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘   │
│  └─────────────────────────────────────────────────────────┘
└──────────────────────────────────────────────────────────────┘
```

---

## 3. 核心子系统架构

### 3.1 内容推荐与 Feed 流水线

```
                       ┌─────────────────┐
                       │   HomeScreen    │
                       │   (Compose UI)  │
                       └────────┬────────┘
                                │ observe Flow/StateFlow
                                ▼
┌──────────────────┐    ┌──────────────────────────────────┐
│ Feed ViewModel   │    │   MixedHomeFeedViewModel         │
│ (HomeFeedVM 等)  ├────┤   (组合多个推荐源)               │
└────────┬─────────┘    └──────────────┬───────────────────┘
         │                              │
         │ PaginationEnvironment        │
         ▼                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              PaginationEnvironment (接口)                       │
│  ┌──────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐ │
│  │ ZhihuApiEnv  │ │ AccountEnv │ │ FeedDisplay │ │ LocalRecEnv │ │
│  └──────┬───────┘ └─────┬──────┘ └─────┬──────┘ └─────┬──────┘ │
│         │                │              │              │        │
└─────────┼────────────────┼──────────────┼──────────────┼────────┘
          │                │              │              │
          ▼                ▼              ▼              ▼
┌──────────────────┐ ┌──────────────┐ ┌──────────────┐ ┌─────────────────┐
│ Ktor HTTP Client │ │ AccountData  │ │ FeedDisplay  │ │ LocalRecommend- │
│ (signFetch)      │ │ (Android)    │ │ Settings     │ │ ationEngine     │
└────────┬─────────┘ └──────────────┘ └──────────────┘ │  (Crawling +    │
         │                                              │   Ranking +     │
         ▼                                              │   Scheduling)   │
┌──────────────────┐                              ┌────┴───────────────┐
│  Zhihu API       │                              │  LocalContent DB  │
│  (Web/Mobile)    │                              │  (Crawling tasks, │
│                  │                              │   results, feeds) │
└──────────────────┘                              └────────────────────┘
          │                                              │
          │              ┌──────────────────────────────┘
          ▼              ▼
┌──────────────────────────────────────────────────────┐
│  Feed 流水线处理                                      │
│  1. fetchZhihuAuthenticatedJson() 获取原始 Feed      │
│  2. flattenFeeds() 展平分组 Feed                     │
│  3. toDisplayItem() 转换为 FeedDisplayItem           │
│  4. applyHomeFeedFilters() 应用内容过滤              │
│  5. recordContentInteraction() 记录曝光/交互         │
└──────────────────────────────────────────────────────┘
```

### 3.2 本地推荐引擎 (LocalRecommendationEngine)

```
┌─────────────────────────────────────────────────────────────┐
│      LocalRecommendationEngine (commonMain)                 │
│                                                             │
│  initialize() ──► startScheduling() ──► 定期爬取知乎推荐    │
│                                                             │
│  generateRecommendations(limit)                            │
│    ├── collectCandidateResults()  ← LocalContentDao        │
│    ├── ensurePendingTasks()       ← 任务队列                │
│    ├── executeHighPriorityTasks() ← CrawlingExecutor       │
│    ├── buildBehaviorProfile()     ← UserBehaviorAnalyzer   │
│    ├── rankCandidate()            ← 评分算法               │
│    │   score = feedScore * reasonWeight * contentWeight * freshWeight │
│    └── applyReasonDiversity()     ← 多样性调节              │
│                                                             │
│  依赖组件:                                                    │
│  ├── LocalContentDao (Room)  ← shared-local-db            │
│  ├── FeedGenerator          ← 生成 LocalFeed              │
│  ├── UserBehaviorAnalyzer   ← 分析用户行为                │
│  ├── CrawlingExecutor       ← 执行爬虫任务                │
│  └── TaskScheduler          ← 调度循环任务                │
└─────────────────────────────────────────────────────────────┘

爬虫任务类型 (CrawlingReason):
  ┌──────────────┬───────────┬────────────┐
  │ Following    │ Trending  │ UpvotedQ   │
  │ (推荐)       │ (热榜)    │ (点赞问题) │
  ├──────────────┼───────────┼────────────┤
  │ FollowingUpv │ CollabFilt│            │
  │ (关注赞同)   │ (协同过滤)│            │
  └──────────────┴───────────┴────────────┘
```

### 3.3 内容过滤系统

```
┌─────────────────────────────────────────────────────────────┐
│                    ContentFilterManager                     │
│  (共用层: viewmodel/filter)                                  │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              ContentFilterDatabase (Room)                    │
│  shared-local-db 模块                                        │
│  ┌─────────────────────────────────────────────────┐        │
│  │  BlockedUserDao          ─ 屏蔽用户            │        │
│  │  BlockedTopicDao         ─ 屏蔽话题            │        │
│  │  BlockedKeywordDao       ─ 屏蔽关键词          │        │
│  │  BlockedQuestionAuthorDao┘ 屏蔽问题作者        │        │
│  │  ContentViewRecordDao    ─ 内容曝光记录        │        │
│  │  ContentOpenEventDao     ─ 内容打开事件        │        │
│  │  BlockedContentRecordDao └ 屏蔽内容记录        │        │
│  └─────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              过滤策略与 NLP                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Keyword 过滤 (Full: HanLP NLP)                      │   │
│  │  ─ KeywordAnalyzerCore                             │   │
│  │  ─ KeywordWeightExtractor (HanLP / Onnx)          │   │
│  │                                                     │   │
│  │ Semantic 过滤 (Full: Sentence Embedding)            │   │
│  │  ─ NlpServiceKeywordSemanticMatcher                │   │
│  │  ─ SentenceEmbeddingManager (Rust tokenizer)       │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ BlocklistBackup  ─ 导入/导出屏蔽列表               │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 3.4 Markdown 渲染与正文浏览

```
          ┌─────────────────────────────────────────┐
          │        ContentDetail API                   │
          │        (Zhihu REST)                       │
          └────────────┬───────────────────────────────┘
                       │ DataHolder.Content
                       ▼
┌──────────────────────────────────────────────────────────┐
│           markdown-renderer (third_party fork)           │
│                                                          │
│  Markdown.kt  ──►  parse Markdown → MdAst                  │
│  MarkdownRuntime.kt ──► Compose 渲染                      │
│                                                          │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐            │
│  │ 代码块渲染 │ │ LaTeX 公式 │ │ 图片/视频  │            │
│  │ (KaTeX)     │ │ (latex-   │ │ (Coil3)    │            │
│  │             │ │ renderer) │ │            │            │
│  └────────────┘ └────────────┘ └────────────┘            │
│                                                          │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐            │
│  │ 段评高亮   │ │脚注导航   │ │选区保持    │            │
│  │ (segment_ │ │ (footnote)  │ │ (selection)│            │
│  │ infos)    │ │             │ │            │            │
│  └────────────┘ └────────────┘ └────────────┘            │
└──────────────────────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│                 ArticleScreen / QuestionScreen           │
│                 (commonMain/ui/)                          │
└──────────────────────────────────────────────────────────┘
```

### 3.5 导航系统

```
┌─────────────────────────────────────────────────────────────┐
│  导航入口 (NavDestination, commonMain/navigation/)           │
│  ─ sealed interface NavDestination                            │
│                                                               │
│  顶层 Tab (TopLevelDestination):                              │
│    Home, Follow, HotList, Daily, OnlineHistory,             │
│    MyCollections, Account, MainTabs                         │
│                                                               │
│  内容详情:                                                    │
│    Article(type, id), Question(id), Pin(id), Video(id)      │
│                                                               │
│  其他:                                                        │
│    Search, Collections, Person, Topic, Notification,        │
│    WriteAnswer, WritePin, Settings 子页面...                │
│                                                               │
│  路由解析: resolveContent(url) ── 支持 URL/deeplink 解析     │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│  ZhihuMain (commonMain/ui/ZhihuMain.kt)                      │
│  ─ 共享主导航壳                                              │
│  ─ 使用 NavHostController + PagerState                       │
│  ─ 每个 NavDestination 注册为 composable 页面                 │
│  ─ 提供 LocalNavigator (CompositionLocal) 给子页面           │
└─────────────────────────────────────────────────────────────┘
         │
         ├────────────────────────────┐
         ▼                            ▼
┌──────────────────────┐  ┌──────────────────────────┐
│ AndroidZhihuMain     │  │ DesktopZhihuMain           │
│ (androidMain/ui/)    │  │ (jvmMain/ui/)              │
│ ─ 传入 ArticleHost   │  │ ─ 传入导航回调             │
│ ─ 传入动画过渡        │  │ ─ 传入动画过渡             │
│ ─ ViewModel 工厂      │  │ ─ ViewModel 工厂           │
└──────────────────────┘  └──────────────────────────┘
```

### 3.6 阅读播放器 (TTS Reading Player)

```
┌─────────────────────────────────────────────────────────────┐
│  ReadingPlayerController (commonMain/reading/)               │
│  ─ 管理阅读队列、播放状态、进度                               │
│  ─ 支持跨内容连续播放 (Article → Pin → Question)             │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  Platform 实现                                              │
│  ┌─────────────────┐  ┌─────────────────┐                   │
│  │ AndroidReading  │  │ JvmReading      │                   │
│  │ PlayerBridge    │  │ Player         │                   │
│  │ (Service)       │  │ (Java Speech)  │                   │
│  └─────────────────┘  └─────────────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

### 3.7 AIGC 投票系统

```
┌─────────────────────────────────────────────────────────────┐
│  客户端 (shared)                                            │
│  ─ AigcVoteClient                                            │
│    ─ syncReadEvent()    ← 上报阅读事件                       │
│    ─ submitFlag()       ← 提交 AIGC 标记                     │
│    ─ getFlagStatus()    ← 查询标记状态                       │
│  ─ AigcVoteVoter / Evidence                                 │
└────────────┬──────────────────────────────────────────────────┘
             │ HTTP API
             ▼
┌─────────────────────────────────────────────────────────────┐
│  AIGC Vote Server (Rust)                                    │
│  aigc-vote-server/                                          │
│  ─ /v1/read-events:batch                                    │
│  ─ /v1/contents/{type}/{id}/aigc-flag                       │
│  ─ /v1/contents/{type}/{id}/aigc-flag (GET)                 │
│  ─ 支持外部投票源 (zhihuai.sx349.xyz)                        │
│  ─ 信用系统 (credit)，防刷机制                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 环境抽象 (Environment Interfaces)

```
┌─────────────────────────────────────────────────────────────────────┐
│  PaginationEnvironment (接口, commonMain/viewmodel/)                │
│  ─ 用于分页 ViewModel 的统一依赖接口，采用接口组合模式               │
└─────────────────────────────────────────────────────────────────────┘
         │ 继承 / 组合
         ├──────────────────────────────────────────────────────────────┐
         ▼                       ▼                       ▼
┌───────────────┐    ┌──────────────────┐    ┌──────────────────┐
│ZhihuApiEnv    │    │AccountEnvironment │    │ContentBlocklist  │
│ (HTTP client)  │    │ (登录/登出)      │    │ Environment      │
└───────┬───────┘    └────────┬─────────┘    └────────┬─────────┘
        │                      │                        │
        ▼                      ▼                        ▼
┌─────────────────────────────────────────────────────────────────────┐
│  ┌──────────────┐ ┌──────────────┐ ┌─────────────┐ ┌─────────────┐│
│  │MobileHomeFeed│ │FeedDisplayEnv │ │ContentInter-│ │AigcVoteEnv  ││
│  │ Environment   │ │ (过滤设置)   │ │ actionEnv   │ │             ││
│  └──────────────┘ └──────────────┘ └─────────────┘ └─────────────┘│
│         │                                              │          │
│         ▼                                              ▼          │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ ProfileLoadEnvironment                                     │   │
│  │  = ContentLoadEnvironment + ContentBlocklistEnvironment    │   │
│  │    = ZhihuApiEnvironment + ContentOpenEnv + AigcVoteEnv   │   │
│  └──────────────────────┬─────────────────────────────────────┘   │
│                         │                                          │
│                         ▼                                          │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ ArticleLoadEnvironment                                     │   │
│  │  = ZhihuApiEnvironment + ContentLoadEnvironment +          │   │
│  │    ArticleNavigationEnvironment                           │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ LocalRecommendationEnvironment                             │   │
│  │  = ZhihuApiEnvironment + local engine access              │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ ArticleExportContentEnvironment                          │   │
│  │  = ArticleExportEnvironment + ZhihuApiEnvironment         │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ ClipboardEnvironment                                       │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ HistoryEnvironment                                         │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ HistoryEnvironment                                         │   │
│  └────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Platform 实现 (expect/actual)                                      │
│  ── rememberPaginationEnvironment()                                 │
│     • Android: AndroidViewmodelSupport.kt                           │
│     • JVM:     PaginationEnvironment.jvm.kt                         │
│     • iOS/Native: (规划中)                                          │
└─────────────────────────────────────────────────────────────────────┘
```

### 环境接口汇总

| 接口名称 | 职责 |
|---------|-----|
| `ZhihuApiEnvironment` | HTTP 客户端、ZSE 签名、API 请求执行 |
| `AccountEnvironment` | 账号登录/登出/刷新/重启应用 |
| `PaginationEnvironment` | 组合所有子环境，提供给分页 ViewModel |
| `FeedDisplayEnvironment` | Feed 显示设置与内容过滤 |
| `ContentLoadEnvironment` | 内容加载 (ZhihuApi + ContentOpen + AigcVote) |
| `ProfileLoadEnvironment` | 个人主页加载 (+ 用户屏蔽) |
| `ArticleLoadEnvironment` | 文章详情加载 (+ 导航切换状态) |
| `ArticleExportEnvironment` | 文章导出 (HTML / 图片) |
| `ContentBlocklistEnvironment` | 用户/问题作者屏蔽 |
| `AigcVoteEnvironment` | AIGC 标记投票客户端 |
| `ClipboardEnvironment` | 剪贴板读写 |
| `HistoryEnvironment` | 本地浏览历史 |
| `MobileHomeFeedEnvironment` | 移动端首页 Feed (Android 专用) |
| `ContentInteractionEnvironment` | 记录内容交互 (点赞等) |
| `ContentOpenEnvironment` | 记录内容打开事件 |
| `LocalRecommendationEnvironment` | 本地推荐引擎访问 |

---

## 5. 数据流总结

```
用户操作 (点击 Feed 条目)
    │
    ▼
NavDestination.resolveContent(url) / NavDestination(Article/Question/Pin)
    │
    ▼
NavController.navigate(route)
    │
    ▼
ZhihuMain composable 分发到对应页面
    │
    ▼
ArticleViewModel / QuestionViewModel (通过 PaginationEnvironment)
    │
    ├─ ZhihuApiEnvironment.fetchJson()
    │   ─ Ktor HttpClient (with ZSE sign)
    │   ─ Zhihu API (www.zhihu.com/api/v4/...)
    │   ─ ContentDetailCache (LRU, 10min)
    │
    ├─ ContentFilterManager (Room)
    │   ─ 检查屏蔽用户/话题/关键词
    │   ─ 记录曝光/交互 (ContentViewRecord)
    │   ─ 记录打开事件 (ContentOpenEvent)
    │
    ├─ LocalRecommendationEngine (Room) [首页本地模式]
    │   ─ 抓取任务 → 爬虫结果 → 评分排序 → 去重
    │
    ├─ AigcVoteClient (AIGC 投票服务器)
    │   ─ 上报阅读证据 / 提交标记
    │
    └─ Markdown Renderer (third_party)
        ─ 解析 HTML → MdAst → Compose 渲染
        ─ LaTeX 公式 (latex-renderer)
        ─ 图片/视频 (Coil3)
        ─ 代码块 (KaTeX 字体)
        ─ 段评高亮 (segment_infos)

ViewModel 输出 StateFlow
    │
    ▼
Compose UI 重组渲染
```

---

## 6. 关键设计模式

### 6.1 expect/actual (KMP 平台抽象)

```
commonMain/                          ← 共享声明
├── platform/PlatformCapabilities.kt  ← expect fun 声明
├── account/ZhihuAccountClient.kt     ← 共享业务逻辑
├── data/AccountData.kt               ← (common) 类型定义
├── markdown/MarkdownRuntime.kt       ← expect @Composable
├── reading/ReadingPlayer.kt          ← expect 接口
├── notification/OnlineHomeNotification.kt ← expect
└── viewmodel/PaginationEnvironment.kt ← expect @Composable

androidMain/ → actual 实现 (Android HttpClient, AccountData, Service, etc.)
jvmMain/     → actual 实现 (DesktopZhihuMain, JVM HttpClient, JavaFX, etc.)
nativeMain/  → actual 实现 (iOS, 暂未完全实现)
iosMain/     → actual 实现 (iOS, 暂未完全实现)
```

### 6.2 环境接口 (Environment Interface)

```kotlin
// PaginationEnvironment 作为依赖注入容器
interface PaginationEnvironment :
    ZhihuApiEnvironment,      // HTTP 访问
    AccountEnvironment,       // 账号管理
    MobileHomeFeedEnvironment,// 移动 Feed
    FeedDisplayEnvironment,   // Feed 显示/过滤
    ContentInteractionEnvironment, // 交互记录
    LocalRecommendationEnvironment, // 本地推荐
    ClipboardEnvironment,     // 剪贴板
    ProfileLoadEnvironment,   // 个人主页
    ArticleLoadEnvironment,   // 文章详情
    ArticleExportContentEnvironment // 导出

// 平台通过 expect/actual 提供具体实现
expect fun rememberPaginationEnvironment(
    allowGuestAccess: Boolean
): PaginationEnvironment
```

### 6.3 ViewModel 继承体系

```
ViewModel (androidx)
  │
  ├── ArticleAnswerSwitchData (回答切换共享状态)
  │
  ├── PaginationViewModel<T> (分页基类)
  │     ├── BaseFeedViewModel (Feed 基类)
  │     │    ├── HomeFeedViewModel
  │     │    ├── AndroidHomeFeedViewModel (Android 专用)
  │     │    ├── MixedHomeFeedViewModel (混合模式)
  │     │    ├── FollowViewModel
  │     │    ├── HotListViewModel
  │     │    ├── DailyViewModel
  │     │    └── SearchViewModel
  │     │
  │     ├── QuestionFeedViewModel
  │     ├── CollectionContentViewModel
  │     ├── OnlineHistoryViewModel
  │     └── NotificationViewModel
  │
  ├── ArticleViewModel (文章/回答详情)
  │
  └── BaseCommentViewModel (评论基类)
        ├── RootCommentViewModel
        └── ChildCommentViewModel
```

---

## 7. 构建变体 (Build Variants)

### :app 模块

| 变体 | 说明 | 特性 |
|------|------|------|
| **full** | 完整版 | 包含 HanLP NLP、Onnx 模型、智能内容过滤 |
| **lite** | 轻量版 | ~4MB，无 ML 功能，仅基础过滤 |

```
app/src/
├── main/       ← 共享 Android 代码 + 资源
│   ├── java/com/github/zly2006/zhihu/
│   │   ├── MainActivity.kt
│   │   ├── LoginActivity.kt
│   │   ├── QRCodeScanActivity.kt
│   │   ├── VideoPlayerActivity.kt
│   │   ├── WebviewActivity.kt
│   │   ├── CaptureActivity.kt
│   │   ├── nlp/         ← HanLP + Onnx (Full)
│   │   │   ├── KeywordAnalyzer.kt
│   │   │   ├── ModelManager.kt
│   │   │   └── NlpServiceKeywordSemanticMatcher.kt
│   │   └── ui/            ← Android 专用 UI (ZhihuMainAndroid*)
│   └── res/ + assets/
├── full/       ← Full 专用 (NLP 依赖声明)
└── lite/       ← Lite 专用 (精简依赖)
```

### shared-local-db 模块

| 源集 | 说明 |
|------|------|
| `commonMain` | Room 实体、DAO、数据库定义 |
| `androidMain` | Android Room 驱动配置 |
| `jvmMain` | JVM SQLite 驱动 (sqlite-bundled) |
| `nativeMain` | Native 驱动 (SQLite) |

---

## 8. 外部服务集成

```
┌─────────────────────┐
│  Zhihu Web API      │
│  www.zhihu.com/    │
│  api.zhihu.com/    │
│  (zse96 v2 签名)   │
└──────────┬──────────┘
           │
           │ Ktor HTTP Client
           │ (AccountData.httpClient)
           ▼
┌────────────────────────────────┐
│  AigcVote Server (Rust)         │
│  (self-hosted, AIGC 标记)      │
└────────────────────────────────┘

┌────────────────────────────────┐
│  GitHub Releases API            │
│  (UpdateManager 版本检查)       │
└────────────────────────────────┘

┌────────────────────────────────┐
│  LaTeX Renderer (fork)          │
│  (zly2006/latex, Maven Central) │
└────────────────────────────────┘

┌────────────────────────────────┐
│  HanLP (Full variant only)     │
│  (com.hankcs:hanlp)            │
└────────────────────────────────┘

┌────────────────────────────────┐
│  Sentence Embeddings (Full)     │
│  (Rust HF Tokenizer)           │
└────────────────────────────────┘
```
