package com.zhihuminus.data.zhihu

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.core.settings.AppSettingsRepository
import com.zhihuminus.data.cache.PostContentCache
import com.zhihuminus.data.local.SearchHistoryStore
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
import com.zhihuminus.data.zhihu.repository.ZhihuCollectionRepository
import com.zhihuminus.data.zhihu.repository.ZhihuColumnRepository
import com.zhihuminus.data.zhihu.repository.ZhihuCommentRepository
import com.zhihuminus.data.zhihu.repository.ZhihuDailyRepository
import com.zhihuminus.data.zhihu.repository.ZhihuFollowRepository
import com.zhihuminus.data.zhihu.repository.ZhihuHistoryRepository
import com.zhihuminus.data.zhihu.repository.ZhihuHomeRepository
import com.zhihuminus.data.zhihu.repository.ZhihuNotificationRepository
import com.zhihuminus.data.zhihu.repository.ZhihuPeopleRepository
import com.zhihuminus.data.zhihu.repository.ZhihuPostRepository
import com.zhihuminus.data.zhihu.repository.ZhihuQuestionRepository
import com.zhihuminus.data.zhihu.repository.ZhihuSearchRepository
import com.zhihuminus.data.zhihu.repository.ZhihuTopicRepository
import com.zhihuminus.feature.collection.CollectionRepository
import com.zhihuminus.feature.column.ColumnRepository
import com.zhihuminus.feature.comment.CommentRepository
import com.zhihuminus.feature.daily.DailyRepository
import com.zhihuminus.feature.follow.FollowRepository
import com.zhihuminus.feature.history.HistoryRepository
import com.zhihuminus.feature.home.HomeRepository
import com.zhihuminus.feature.notification.NotificationRepository
import com.zhihuminus.feature.people.PeopleRepository
import com.zhihuminus.feature.post.PostRepository
import com.zhihuminus.feature.question.QuestionRepository
import com.zhihuminus.feature.search.SearchRepository
import com.zhihuminus.feature.topic.TopicRepository
import kotlinx.io.files.Path

class ZhihuRepositoryFactory(
    private val environment: ZhihuApiEnvironment,
    val postCache: PostContentCache = PostContentCache,
) {
    val postApi: ZhihuPostApi by lazy { ZhihuPostApi(environment) }
    val questionApi: ZhihuQuestionApi by lazy { ZhihuQuestionApi(environment) }
    val feedApi: ZhihuFeedApi by lazy { ZhihuFeedApi(environment) }
    val commentApi: ZhihuCommentApi by lazy { ZhihuCommentApi(environment) }
    val collectionApi: ZhihuCollectionApi by lazy { ZhihuCollectionApi(environment) }
    val peopleApi: ZhihuPeopleApi by lazy { ZhihuPeopleApi(environment, feedApi) }
    val notificationApi: ZhihuNotificationApi by lazy { ZhihuNotificationApi(environment) }
    val historyApi: ZhihuHistoryApi by lazy { ZhihuHistoryApi(environment) }
    val topicApi: ZhihuTopicApi by lazy { ZhihuTopicApi(environment) }
    val dailyApi: ZhihuDailyApi by lazy { ZhihuDailyApi(environment) }
    val columnApi: ZhihuColumnApi by lazy { ZhihuColumnApi(environment) }
    val searchApi: ZhihuSearchApi by lazy { ZhihuSearchApi(environment) }

    fun createPostRepository(): PostRepository =
        ZhihuPostRepository(
            postApi = postApi,
            collectionApi = collectionApi,
            peopleApi = peopleApi,
            historyApi = historyApi,
            postCache = postCache,
        )

    fun createQuestionRepository(): QuestionRepository =
        ZhihuQuestionRepository(
            questionApi = questionApi,
            historyApi = historyApi,
            feedApi = feedApi,
            postCache = postCache,
        )

    fun createCommentRepository(): CommentRepository =
        ZhihuCommentRepository(commentApi)

    fun createCollectionRepository(): CollectionRepository =
        ZhihuCollectionRepository(collectionApi)

    fun createColumnRepository(): ColumnRepository =
        ZhihuColumnRepository(columnApi)

    fun createDailyRepository(): DailyRepository =
        ZhihuDailyRepository(dailyApi)

    fun createFollowRepository(): FollowRepository =
        ZhihuFollowRepository(
            api = feedApi,
            postCache = postCache,
        )

    fun createHistoryRepository(): HistoryRepository =
        ZhihuHistoryRepository(historyApi)

    fun createHomeRepository(startupCacheFile: Path? = null): HomeRepository =
        ZhihuHomeRepository(
            feedApi = feedApi,
            notificationApi = notificationApi,
            startupCacheFile = startupCacheFile,
            postCache = postCache,
        )

    fun createNotificationRepository(): NotificationRepository =
        ZhihuNotificationRepository(notificationApi)

    fun createPeopleRepository(): PeopleRepository =
        ZhihuPeopleRepository(
            api = peopleApi,
            historyApi = historyApi,
        )

    fun createSearchRepository(
        settingsRepository: AppSettingsRepository,
        historyStorage: SearchHistoryStore,
    ): SearchRepository =
        ZhihuSearchRepository(
            searchApi = searchApi,
            topicApi = topicApi,
            peopleApi = peopleApi,
            settingsRepository = settingsRepository,
            historyStorage = historyStorage,
        )

    fun createTopicRepository(): TopicRepository =
        ZhihuTopicRepository(topicApi)
}
