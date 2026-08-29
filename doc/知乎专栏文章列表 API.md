# 知乎专栏文章列表 API

> 本文根据一次浏览器 HAR 抓包整理。
>
> **接口类型：** 知乎专栏文章列表  
> **用途：** 获取指定专栏的文章列表及文章正文 HTML  
> **来源：** 浏览器访问知乎专栏页面时产生的 API 请求
>
> **重要：** HAR 中包含浏览器会话 Cookie 和知乎请求签名等敏感信息。实现客户端时不得硬编码 HAR 中的 Cookie、`x-zse-96`、`x-zst-81` 等具体值。本文只记录其协议结构。

---

## 1. Endpoint

```http
GET https://www.zhihu.com/api/v4/columns/{column_id}/items
```

其中 `{column_id}` 是专栏 ID。

例如：

```text
c_1542809288766390272
```

对应：

```http
GET https://www.zhihu.com/api/v4/columns/c_1542809288766390272/items
```

---

## 2. Query Parameters

| 参数 | 类型 | 示例 | 说明 |
|---|---|---|---|
| `limit` | integer | `10` | 请求的文章数量 |
| `offset` | integer | `10` | 分页偏移量 |
| `ws_qiangzhisafe` | integer | `0` | 知乎内部参数，目前用途未知 |

典型请求：

```http
GET /api/v4/columns/{column_id}/items?limit=10&offset=0&ws_qiangzhisafe=0
```

第二页：

```http
GET /api/v4/columns/{column_id}/items?limit=10&offset=10&ws_qiangzhisafe=0
```

第三页：

```http
GET /api/v4/columns/{column_id}/items?limit=10&offset=20&ws_qiangzhisafe=0
```

---

## 3. Pagination

响应中的 `paging` 对象：

```json
{
  "paging": {
    "is_end": false,
    "totals": 26,
    "previous": "...offset=0",
    "is_start": false,
    "next": "...offset=20"
  }
}
```

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `is_end` | boolean | 是否已经到最后一页 |
| `is_start` | boolean | 是否为第一页 |
| `totals` | integer | 专栏文章总数 |
| `previous` | string | 上一页 URL |
| `next` | string | 下一页 URL |

### 推荐实现

客户端不要依赖自己计算 `offset`，而应该优先使用服务端返回的：

```text
paging.next
```

逻辑：

```text
第一次：
/columns/{id}/items?limit=10&offset=0

       ↓

response.paging.next

       ↓

直接请求 next URL

       ↓

response.paging.next

       ↓

直到 paging.is_end == true
```

这样可以避免知乎未来修改分页规则导致客户端失效。

---

# 4. Response Structure

响应：

```json
{
  "paging": {
    "is_end": false,
    "is_start": false,
    "totals": 26,
    "previous": "...",
    "next": "..."
  },
  "data": [
    {
      ...
    }
  ]
}
```

其中：

```text
data
```

是文章列表。

---

# 5. Article Item

一次返回的文章对象包含以下主要字段：

```json
{
  "id": "616099137",
  "title": "...",
  "title_image": "",
  "url": "https://zhuanlan.zhihu.com/p/616099137",
  "excerpt": "...",
  "content": "<h2>...</h2><p>...</p>",
  "author": {...},

  "created": 1679471225,
  "updated": 1679471225,

  "voteup_count": 120,
  "comment_count": 5,

  "article_type": "normal",
  "copyright_permission": "need_review",
  "comment_permission": "all",

  "has_column": true,
  "is_labeled": false,
  "voting": 0,
  "admin_closed_comment": false,
  "force_login_when_click_read_more": false,

  "settings": {
    "table_of_contents": {
      "enabled": true
    }
  }
}
```

---

## 6. Important Article Fields

### `id`

文章 ID。

例如：

```json
"id": "616099137"
```

对应文章：

```text
https://zhuanlan.zhihu.com/p/616099137
```

---

### `title`

文章标题。

```json
"title": "..."
```

---

### `url`

文章网页 URL。

```json
"url": "https://zhuanlan.zhihu.com/p/616099137"
```

---

### `excerpt`

文章摘要。

该字段适合用于专栏文章列表中的预览文本。

---

### `content`

**这是本 API 最重要的字段。**

它不是摘要，而是文章的完整 HTML 正文。

例如：

```json
"content": "<h2>前言</h2><p>...</p><ul>...</ul>"
```

因此：

> **专栏列表 API 本身就可以直接提供文章正文，不需要根据 `id` 再请求一次文章详情接口。**

对于客户端而言，可以直接：

```text
API
 ↓
item.content
 ↓
HTML Parser
 ↓
Content AST
 ↓
Compose Renderer
```

---

# 7. `content` 的 HTML 格式

正文是知乎自己的 HTML。

常见元素包括：

```html
<h2>标题</h2>

<p data-pid="...">
    正文
</p>

<ul>
    <li data-pid="...">列表项</li>
</ul>

<figure data-size="normal">
    <img
        src="..."
        data-caption=""
        data-size="normal"
        data-rawwidth="670"
        data-rawheight="370"
        class="origin_image zh-lightbox-thumb"
        width="670"
        data-original="..."
        data-original-token="..."
    />
</figure>
```

还可能出现代码：

```html
<div class="highlight">
    <pre>
        <code class="language-cpp">
            ...
        </code>
    </pre>
</div>
```

因此正文应该交给现有的 HTML → AST 解析器处理，而不是当作纯文本显示。

---

# 8. Image Fields

正文中的图片通常至少存在：

```html
<img
    src="..."
    data-original="..."
/>
```

例如：

```html
<img
    src="https://pic4.zhimg.com/..._1440w.jpg"
    data-original="https://pic4.zhimg.com/..._r.jpg"
/>
```

交给ast与renderer即可。

---

# 9. Author

文章包含作者对象：

```json
{
  "author": {
    "id": "...",
    "name": "...",
    "url_token": "...",
    "avatar_url": "...",
    "avatar_url_template": "...",
    "headline": "...",
    "description": "...",
    "user_type": "people",
    "is_followed": false,
    "is_following": false,
    "is_org": false
  }
}
```

客户端如果只需要展示文章列表，可以只保存：

```kotlin
data class Author(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val headline: String?,
)
```

---

# 10. Suggested Kotlin Models

推荐的数据模型：

```kotlin
data class ColumnResponse(
    val paging: Paging,
    val data: List<ColumnArticle>,
)

data class Paging(
    val isEnd: Boolean,
    val isStart: Boolean,
    val totals: Int,
    val previous: String?,
    val next: String?,
)

data class ColumnArticle(
    val id: String,
    val title: String,
    val titleImage: String?,
    val url: String,
    val excerpt: String?,
    val content: String?,
    val author: Author?,
    val created: Long,
    val updated: Long,
    val voteupCount: Int,
    val commentCount: Int,
    val articleType: String?,
    val commentPermission: String?,
    val hasColumn: Boolean,
    val settings: ArticleSettings?,
)

data class ArticleSettings(
    val tableOfContents: TableOfContents?,
)

data class TableOfContents(
    val enabled: Boolean,
)

data class Author(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val avatarUrlTemplate: String?,
    val headline: String?,
)
```

实际项目中可以根据 UI 需要进一步裁剪字段。尽量复用现有的数据结构，因为专栏中只有文章，也就是Post的article类型。

---

# 11. Client Architecture

推荐将网络层和正文解析层分开：

```text
Column API
    │
    ▼
ColumnResponse
    │
    ├── paging
    │
    └── data
         │
         ▼
    ColumnArticle
         │
         │ content
         ▼
    HTML Parser
         │
         ▼
       AST
         │
         ▼
   Compose Renderer
```

不要让 API Model 直接承担 Compose 渲染职责。

例如：

```kotlin
suspend fun getColumnItems(
    columnId: String,
    nextUrl: String? = null,
): ColumnResponse
```

然后：

```kotlin
val response = api.getColumnItems(columnId)

for (article in response.data) {
    val nodes = astParser.parseContent(article.content)
    render(nodes)
}
```

---

# 12. Request Headers

HAR 中观察到以下请求头：

```http
User-Agent: <browser user agent>
Accept: */*
Accept-Language: zh-CN
Referer: https://www.zhihu.com/column/{column_id}
x-requested-with: fetch

x-zse-93: 101_3_3.0
x-zse-96: <dynamic value>
x-zst-81: <dynamic value>
```

其中：

### `Referer`

浏览器请求来自对应的专栏页面：

```text
https://www.zhihu.com/column/{column_id}
```

### `x-requested-with`

HAR 中为：

```text
fetch
```

### `x-zse-93`

HAR 中：

```text
101_3_3.0
```

这是知乎请求签名体系的一部分。

### `x-zse-96`

动态值。

### `x-zst-81`

动态值。

---

# 13. Authentication / Anti-Abuse

HAR 中同时存在 Cookie，例如：

```http
Cookie: ...
```

但 Cookie 是浏览器会话状态，**不能写入客户端源码，也不能把 HAR 中的 Cookie 当成 API 常量使用。**

此外：

```text
x-zse-96
x-zst-81
```

看起来属于知乎的动态请求签名/风控机制。

目前这个 HAR **只能证明浏览器成功请求时存在这些字段**，不能仅凭一次请求确定：

1. 哪些字段是绝对必须的；
2. 哪些字段可以省略；
3. 签名具体如何计算；
4. 签名是否与 URL、Query、Cookie、请求时间等因素有关；
5. Android 客户端是否能够直接复用浏览器请求。

因此实现 API Client 时，不要假设这些值可以固定。

---

# 14. 已知信息

- Endpoint：

```text
GET /api/v4/columns/{column_id}/items
```

- 支持 `limit`
- 支持 `offset`
- 存在 `ws_qiangzhisafe`
- 返回分页信息
- 返回文章总数
- 返回文章元数据
- 返回文章摘要
- **返回文章正文 HTML**
- 正文包含图片、列表、标题、代码块等 HTML
- `paging.next` 可用于继续分页
- 需要使用 zse 加密，目前项目已提供相关能力

---

# 15. Implementation Notes for AI Agent

1. 将此接口封装在 Repository/API 层，不要在 Composable 中直接请求。
2. 你可以先不做缓存，让用户每次点开都请求对应的数据
3. 使用 `paging.next` 进行分页。
4. 不要把 HAR 中的 Cookie、`x-zse-96`、`x-zst-81` 固化到代码中。
5. 不要假设 `ws_qiangzhisafe` 的语义，除非有进一步抓包证据。
6. 网络 Model 与 AST / UI Model 分离。
7. `content` 可能很大，不建议在 UI 层重复解析；可以在数据层或 ViewModel 层完成解析并缓存。
8. 图片 URL 应由统一的图片加载/缓存层处理。
9. 对 `content == null`、空字符串以及 HTML 结构变化保持容错。
10. 分页结束条件优先使用：

```kotlin
response.paging.isEnd
```

11. 下一页优先使用：

```kotlin
response.paging.next
```

而不是自行拼接 offset。
