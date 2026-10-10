package com.zhihuminus.data.zhihu

import com.zhihuminus.data.zhihu.api.ZhihuCollectionApi
import com.zhihuminus.data.zhihu.api.ZhihuColumnApi
import com.zhihuminus.data.zhihu.api.ZhihuCommentApi
import com.zhihuminus.data.zhihu.api.ZhihuDailyApi
import com.zhihuminus.data.zhihu.api.ZhihuFeedApi
import com.zhihuminus.data.zhihu.api.ZhihuHistoryApi
import com.zhihuminus.data.zhihu.api.ZhihuNotificationApi
import com.zhihuminus.data.zhihu.api.ZhihuPeopleApi
import com.zhihuminus.data.zhihu.api.ZhihuPostApi
import com.zhihuminus.data.zhihu.api.ZhihuQuestionApi
import com.zhihuminus.data.zhihu.api.ZhihuSearchApi
import com.zhihuminus.data.zhihu.api.ZhihuTopicApi

/**
 * 知乎 API 综合入口契约。
 * 通过接口继承聚合各个细粒度的领域 API，既保持 100% 向后兼容，又支持各业务仓储遵循接口隔离原则（ISP）按需依赖。
 */
interface ZhihuApi :
    ZhihuPostApi,
    ZhihuQuestionApi,
    ZhihuFeedApi,
    ZhihuCommentApi,
    ZhihuCollectionApi,
    ZhihuPeopleApi,
    ZhihuNotificationApi,
    ZhihuHistoryApi,
    ZhihuTopicApi,
    ZhihuDailyApi,
    ZhihuColumnApi,
    ZhihuSearchApi
