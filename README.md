# Zhihu--

> 本项目是 [zly2006/zhihu-plus-plus](https://github.com/zly2006/zhihu-plus-plus) 的一个分支（fork），在原作者 Liyan Zhao (zly2006) 的基础上经过大幅修改。
>
> **原项目仍在积极维护**，如果你需要完整的功能（AI 智能过滤、桌面版、本地推荐算法等），请移步原仓库。
>
> 此分支主要聚焦以下方向：
> - 移除 KMP / 桌面端支持，**Android 端仅化**
> - 移除 AI / NLP / onnx 等功能（包括智能内容过滤、AIGC 标记、AI 总结）
> - 移除阅读朗读、双击手势、文章 WebView 等功能
> - 移除内容创作（写回答 / 发想法）
> - 重写内容解析渲染逻辑（基于自研 AST + Parser + Renderer）
> - 使用 Media3 (ExoPlayer) 播放视频
> - 默认 Material3 (Duo3) 样式

> 本项目还不够完善，欢迎 PR。

知乎第三方 Android 客户端。

> ⚠️ 此分支是从原项目的一次历史快照衍生的，**并非原项目的最新版本**。
> 许多原有功能在此分支中已被移除或重构，功能完备度可能低于原项目。

## 下载

目前暂不提供发行版安装包。

## 路线图

### 可用功能

- 登录与账号
  - 支持手机验证码登录
  - 支持通过扫码在电脑端登录
  - 支持手动设置 Cookie 登录
- 信息流与推荐
  - 支持切换 **登录状态 / 非登录状态** 下的推荐，防止信息茧房
  - 支持关注页（推荐/动态，可显示动态来源说明）、热榜、知乎日报、搜索（含热搜、历史、排序/类型/时间筛选）
  - 信息流自动过滤广告条目
- 内容浏览
  - 阅读回答
  - 阅读文章
  - 浏览问题详情页（排序、关注、日志 WebView 弹窗浏览、分享、评论）
  - 浏览想法（Pin）详情页（点赞、评论、分享、话题、投票，多图画廊浏览）
  - 信息流卡片摘要基于 AST 解析渲染
  - 文章与想法采用统一的 Post 阅读页（正文可选中复制、顶栏标题随滚动浮现）
  - 浏览收藏夹及收藏夹内容
  - 在用户主页内搜索 TA 的创作
  - 历史记录（在线历史 + 本地历史，支持删除）
  - 展示知乎官方认证徽章
  - 应用内播放知乎视频（基于 Media3 ExoPlayer）
- 阅读
  - 回答页长按保存图片 **无水印**
  - **导出内容**（PDF / 图片）
  - 图片查看器支持动图（GIF）与多图滑动切换
  - 数学公式渲染（LaTeX，支持行内公式，深色模式下随主题着色）
  - 代码块使用内置等宽字体
  - 阅读页自动隐藏的滚动进度条
- 社区互动
  - 支持查看个人主页（含关注订阅板块）、关注/拉黑用户、屏蔽推荐
  - 支持查看回答赞同者列表，以及关注的人赞同
  - 支持赞同 / 反对 / 点赞
  - 评论区（含子评论、回复、点赞、按时间排序）
  - 通过链接跳转时自动定位到指定评论并高亮
  - 头像点击跳转作者主页，回答标题点击跳转问题详情页
  - 通知（支持分类、红点设置、全部标记已读、自动标记已读与通知筛选）
  - 表情包
- 其他
  - 动态图标（Android 12+ Material You 系统取色，Android 11- 使用固定配色）
  - 支持 zse96 v2 签名算法（可以调用 99% 的网页端 API）
  - 支持模拟安卓端 API 调用
  - 支持 Deep Link 与剪贴板链接识别跳转；注册 `zhminus://` 应用内路由，知乎内容链接直接在应用内打开而不跳转浏览器
  - 支持二维码扫码结果展示和复制，可用于提取网址、Wi-Fi 密码等信息
  - 主界面支持横滑切换标签页
  - 支持自定义初始页面
  - 点击底部导航栏回到顶部/刷新
- 内容渲染
  - 采用 AST + Parser + Renderer 渲染
  - 支持段落、标题、分割线、链接、代码块、引用、图片、视频、列表、表格等内容节点
  - 支持文本、加粗 / 斜体与 Emoji 行内节点，支持行内 LaTeX 公式
  - 引用块指示器使用 drawBehind 绘制
  - 支持图片预览功能
  - HTML 解析统一基于 Jsoup

## 架构调整 (相比原项目)

| 项目 | 原项目 (zly2006)                | 此分支 (Vodyanitssa) |
| --- |------------------------------| --- |
| 模块架构 | KMP (shared + desktopApp)    | 仅 Android 端 (app) |
| 包名 | `com.github.zly2006.zhplus`  | `com.zhihuminus` |
| minSdk | 27                           | 28 |
| 视频播放 | MediaPlayer                  | Media3 ExoPlayer |
| 内容渲染 | WebView / Markdown 解析后渲染     | AST + Compose 渲染 |
| 启动图标 | 静态图 (ic_launcher)            | 自适应图标 + Material You 动态取色 (ic_zhihuminus_launcher) |
| AI / NLP | 支持 (onnx, sentence_embeddings) | 移除 |
| 桌面版 | 支持                           | 移除 |
| 本地推荐算法 | 支持                           | 移除 |
| 阅读朗读 | 支持                           | 移除 |
| 双击手势 | 支持                           | 移除 |
| 遥测 | 支持                           | 移除 |
| 内容创作（写回答 / 发想法） | 支持                           | 移除 |
| 模块组织 | 单一 App 包                     | 按 feature 划分（post / question / comment / imageview） |

## 贡献者

感谢所有为 Zhihu++ 做出贡献的开发者与用户，正是你们让这个项目持续变得更好。

[![Contributors](https://ghcontrib.pages.dev/image?repo=Vodyanitssa/zhihu-plus-plus)](https://github.com/Vodyanitssa/zhihu-plus-plus/graphs/contributors)

## 致谢

本项目基于 [zly2006/zhihu-plus-plus](https://github.com/zly2006/zhihu-plus-plus) 开发，感谢原作者 Liyan Zhao (zly2006) 和所有原项目贡献者的辛勤付出。
