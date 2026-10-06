package com.zhihuminus.feature.people

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.core.util.Log
import com.zhihuminus.core.util.friendlyErrorMessage
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.navigation.Person
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class PaginatedTabState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEnd: Boolean = false,
    val errorMessage: String? = null,
    val hasLoaded: Boolean = false,
)

data class PeopleUiState(
    val profile: PeopleProfile = PeopleProfile(),
    val isLoadingProfile: Boolean = false,
    val profileErrorMessage: String? = null,
    val answersSort: String = "voteups",
    val articlesSort: String = "created",
    val answersState: PaginatedTabState<PeopleCreationItem> = PaginatedTabState(),
    val articlesState: PaginatedTabState<PeopleCreationItem> = PaginatedTabState(),
    val activitiesState: PaginatedTabState<FeedDisplayItem> = PaginatedTabState(),
    val collectionsState: PaginatedTabState<Collection> = PaginatedTabState(),
    val questionsState: PaginatedTabState<FollowedQuestion> = PaginatedTabState(),
    val pinsState: PaginatedTabState<PeopleCreationItem> = PaginatedTabState(),
    val columnsState: PaginatedTabState<PeopleColumnItem> = PaginatedTabState(),
    val followersState: PaginatedTabState<PeopleMemberItem> = PaginatedTabState(),
    val followingState: PaginatedTabState<PeopleMemberItem> = PaginatedTabState(),
    val selectedSubscriptionTab: Int = 0,
    val selectedCreationTab: PeopleCreationTab = PeopleCreationTab.Answers,
    val followingColumnsState: PaginatedTabState<PeopleColumnItem> = PaginatedTabState(),
    val followingTopicsState: PaginatedTabState<FollowedTopic> = PaginatedTabState(),
    val followingQuestionsState: PaginatedTabState<FollowedQuestion> = PaginatedTabState(),
    val followingCollectionsState: PaginatedTabState<Collection> = PaginatedTabState(),
    val changingItemFollowIds: Set<String> = emptySet(),
)

fun peopleScreenInitialPage(person: Person): Int {
    val selection = resolvePeopleInitialSelection(person.jumpTo)
    return selection.primaryTab.ordinal
}

class PeopleViewModel(
    val person: Person,
    private val repository: PeopleRepository,
) : ViewModel() {
    var uiState by mutableStateOf(
        PeopleUiState(
            profile = PeopleProfile(
                id = person.id,
                urlToken = person.urlToken,
                name = person.name,
            ),
        ),
    )
        private set

    private val _effect = Channel<PeopleEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val nextUrls = mutableMapOf<String, String?>()
    private val tabJobs = mutableMapOf<String, Job?>()

    val tokenOrId: String
        get() = uiState.profile.urlToken.takeIf { it.isNotBlank() }
            ?: person.urlToken.takeIf { it.isNotBlank() }
            ?: uiState.profile.id.takeIf { it.isNotBlank() }
            ?: person.id

    val memberId: String
        get() = uiState.profile.id.takeIf { it.isNotBlank() && it != Person.EMPTY_ID }
            ?: person.id

    init {
        loadProfile()
        val initial = resolvePeopleInitialSelection(person.jumpTo)
        loadPrimaryTabIfNeeded(initial.primaryTab)
        if (initial.primaryTab == PeoplePrimaryTab.Creations) {
            uiState = uiState.copy(selectedCreationTab = initial.creationTab)
            loadCreationTabIfNeeded(initial.creationTab)
        }
        initial.initialUserListType?.let {
            loadUserListIfNeeded(it)
        }
    }

    fun onEvent(event: PeopleEvent) {
        when (event) {
            is PeopleEvent.RefreshProfile -> loadProfile()
            is PeopleEvent.ToggleFollow -> toggleFollow()
            is PeopleEvent.ToggleItemFollow -> toggleItemFollow(event.people)
            is PeopleEvent.ToggleBlock -> toggleBlock()
            is PeopleEvent.PrimaryTabSelected -> loadPrimaryTabIfNeeded(event.tab)
            is PeopleEvent.CreationTabSelected -> {
                uiState = uiState.copy(selectedCreationTab = event.tab)
                loadCreationTabIfNeeded(event.tab)
            }
            is PeopleEvent.LoadMoreCreation -> loadCreationTab(event.tab, reset = false)
            is PeopleEvent.RefreshCreation -> loadCreationTab(event.tab, reset = true)
            is PeopleEvent.ChangeAnswersSort -> changeAnswersSort(event.sortBy)
            is PeopleEvent.ChangeArticlesSort -> changeArticlesSort(event.sortBy)
            is PeopleEvent.LoadMorePrimary -> loadPrimaryTab(event.tab, reset = false)
            is PeopleEvent.RefreshPrimary -> loadPrimaryTab(event.tab, reset = true)
            is PeopleEvent.LoadUserListIfNeeded -> loadUserListIfNeeded(event.type)
            is PeopleEvent.LoadMoreUserList -> loadUserList(event.type, reset = false)
            is PeopleEvent.RefreshUserList -> loadUserList(event.type, reset = true)
            is PeopleEvent.SubscriptionTabSelected -> selectSubscriptionTab(event.index)
            is PeopleEvent.LoadMoreSubscription -> loadSubscriptionTab(event.index, reset = false)
            is PeopleEvent.RefreshSubscription -> loadSubscriptionTab(event.index, reset = true)
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingProfile = true, profileErrorMessage = null)
            try {
                val profile = repository.getProfile(person.userTokenOrId)
                uiState = uiState.copy(
                    profile = profile,
                    isLoadingProfile = false,
                )
                if (profile.id.isNotBlank()) person.id = profile.id
                if (profile.urlToken.isNotBlank()) person.urlToken = profile.urlToken
                _effect.send(
                    PeopleEffect.ProfileLoaded(
                        Person(
                            id = profile.id,
                            urlToken = profile.urlToken,
                            name = profile.name,
                        ),
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to load member profile", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    isLoadingProfile = false,
                    profileErrorMessage = msg,
                )
                _effect.send(PeopleEffect.ShowMessage("加载用户信息失败: $msg"))
            }
        }
    }

    fun toggleFollow() {
        val urlToken = tokenOrId
        if (urlToken.isBlank()) return
        val currentFollowing = uiState.profile.isFollowing
        val currentCount = uiState.profile.followerCount
        viewModelScope.launch {
            try {
                val newCount = if (!currentFollowing) {
                    repository.follow(urlToken) ?: (currentCount + 1)
                } else {
                    repository.unfollow(urlToken) ?: (currentCount - 1).coerceAtLeast(0)
                }
                uiState = uiState.copy(
                    profile = uiState.profile.copy(
                        isFollowing = !currentFollowing,
                        followerCount = newCount,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to toggle follow", e)
                _effect.send(PeopleEffect.ShowMessage("操作失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }

    fun toggleItemFollow(people: PeopleMemberItem) {
        val targetId = people.id
        val token = people.urlToken.takeIf { it.isNotBlank() } ?: targetId
        if (token.isBlank() || targetId in uiState.changingItemFollowIds) return
        val willFollow = !people.isFollowing

        fun updateList(list: List<PeopleMemberItem>): List<PeopleMemberItem> = list.map { item ->
            if (item.id == targetId || (item.urlToken.isNotBlank() && item.urlToken == people.urlToken)) {
                item.copy(
                    isFollowing = willFollow,
                    followerCount = (item.followerCount + if (willFollow) 1 else -1).coerceAtLeast(0),
                )
            } else {
                item
            }
        }

        val prevFollowers = uiState.followersState.items
        val prevFollowing = uiState.followingState.items

        uiState = uiState.copy(
            changingItemFollowIds = uiState.changingItemFollowIds + targetId,
            followersState = uiState.followersState.copy(items = updateList(prevFollowers)),
            followingState = uiState.followingState.copy(items = updateList(prevFollowing)),
        )

        viewModelScope.launch {
            try {
                if (willFollow) {
                    repository.follow(token)
                } else {
                    repository.unfollow(token)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to toggle item follow", e)
                uiState = uiState.copy(
                    followersState = uiState.followersState.copy(items = prevFollowers),
                    followingState = uiState.followingState.copy(items = prevFollowing),
                )
                _effect.send(PeopleEffect.ShowMessage(if (willFollow) "关注失败" else "取消关注失败"))
            } finally {
                uiState = uiState.copy(changingItemFollowIds = uiState.changingItemFollowIds - targetId)
            }
        }
    }

    fun toggleBlock() {
        val urlToken = tokenOrId
        if (urlToken.isBlank()) return
        val currentBlocking = uiState.profile.isBlocking
        viewModelScope.launch {
            try {
                if (!currentBlocking) {
                    repository.block(urlToken)
                } else {
                    repository.unblock(urlToken)
                }
                uiState = uiState.copy(
                    profile = uiState.profile.copy(
                        isBlocking = !currentBlocking,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to toggle block", e)
                _effect.send(PeopleEffect.ShowMessage("操作失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }

    fun loadPrimaryTabIfNeeded(tab: PeoplePrimaryTab) {
        when (tab) {
            PeoplePrimaryTab.Creations -> loadCreationTabIfNeeded(uiState.selectedCreationTab)
            PeoplePrimaryTab.Activities -> if (!uiState.activitiesState.hasLoaded && !uiState.activitiesState.isLoading) {
                loadActivities(reset = true)
            }
            PeoplePrimaryTab.Collections -> if (!uiState.collectionsState.hasLoaded && !uiState.collectionsState.isLoading) {
                loadCollections(reset = true)
            }
            PeoplePrimaryTab.FollowingSubscriptions -> loadSubscriptionTabIfNeeded(uiState.selectedSubscriptionTab)
        }
    }

    private fun loadPrimaryTab(tab: PeoplePrimaryTab, reset: Boolean) {
        when (tab) {
            PeoplePrimaryTab.Creations -> loadCreationTab(uiState.selectedCreationTab, reset)
            PeoplePrimaryTab.Activities -> loadActivities(reset)
            PeoplePrimaryTab.Collections -> loadCollections(reset)
            PeoplePrimaryTab.FollowingSubscriptions -> loadSubscriptionTab(uiState.selectedSubscriptionTab, reset)
        }
    }

    fun loadCreationTabIfNeeded(tab: PeopleCreationTab) {
        when (tab) {
            PeopleCreationTab.Answers -> if (!uiState.answersState.hasLoaded && !uiState.answersState.isLoading) {
                loadAnswers(reset = true)
            }
            PeopleCreationTab.Articles -> if (!uiState.articlesState.hasLoaded && !uiState.articlesState.isLoading) {
                loadArticles(reset = true)
            }
            PeopleCreationTab.Pins -> if (!uiState.pinsState.hasLoaded && !uiState.pinsState.isLoading) {
                loadPins(reset = true)
            }
            PeopleCreationTab.Columns -> if (!uiState.columnsState.hasLoaded && !uiState.columnsState.isLoading) {
                loadColumns(reset = true)
            }
            PeopleCreationTab.Questions -> if (!uiState.questionsState.hasLoaded && !uiState.questionsState.isLoading) {
                loadQuestions(reset = true)
            }
        }
    }

    private fun loadCreationTab(tab: PeopleCreationTab, reset: Boolean) {
        when (tab) {
            PeopleCreationTab.Answers -> loadAnswers(reset)
            PeopleCreationTab.Articles -> loadArticles(reset)
            PeopleCreationTab.Pins -> loadPins(reset)
            PeopleCreationTab.Columns -> loadColumns(reset)
            PeopleCreationTab.Questions -> loadQuestions(reset)
        }
    }

    fun loadUserListIfNeeded(type: PeopleUserListType) {
        when (type) {
            PeopleUserListType.Following -> if (!uiState.followingState.hasLoaded && !uiState.followingState.isLoading) {
                loadFollowing(reset = true)
            }
            PeopleUserListType.Followers -> if (!uiState.followersState.hasLoaded && !uiState.followersState.isLoading) {
                loadFollowers(reset = true)
            }
        }
    }

    private fun loadUserList(type: PeopleUserListType, reset: Boolean) {
        when (type) {
            PeopleUserListType.Following -> loadFollowing(reset)
            PeopleUserListType.Followers -> loadFollowers(reset)
        }
    }

    private fun selectSubscriptionTab(index: Int) {
        uiState = uiState.copy(selectedSubscriptionTab = index)
        loadSubscriptionTabIfNeeded(index)
    }

    private fun loadSubscriptionTabIfNeeded(subIndex: Int) {
        when (subIndex) {
            0 -> if (!uiState.followingColumnsState.hasLoaded && !uiState.followingColumnsState.isLoading) {
                loadFollowingColumns(reset = true)
            }
            1 -> if (!uiState.followingTopicsState.hasLoaded && !uiState.followingTopicsState.isLoading) {
                loadFollowingTopics(reset = true)
            }
            2 -> if (!uiState.followingQuestionsState.hasLoaded && !uiState.followingQuestionsState.isLoading) {
                loadFollowingQuestions(reset = true)
            }
            3 -> if (!uiState.followingCollectionsState.hasLoaded && !uiState.followingCollectionsState.isLoading) {
                loadFollowingCollections(reset = true)
            }
        }
    }

    private fun loadSubscriptionTab(subIndex: Int, reset: Boolean) {
        when (subIndex) {
            0 -> loadFollowingColumns(reset)
            1 -> loadFollowingTopics(reset)
            2 -> loadFollowingQuestions(reset)
            3 -> loadFollowingCollections(reset)
        }
    }

    private fun changeAnswersSort(newSort: String) {
        if (uiState.answersSort == newSort) return
        uiState = uiState.copy(
            answersSort = newSort,
            answersState = PaginatedTabState(),
        )
        loadAnswers(reset = true)
    }

    private fun changeArticlesSort(newSort: String) {
        if (uiState.articlesSort == newSort) return
        uiState = uiState.copy(
            articlesSort = newSort,
            articlesState = PaginatedTabState(),
        )
        loadArticles(reset = true)
    }

    fun loadAnswers(reset: Boolean) {
        val key = "answers"
        if (uiState.answersState.isLoadingMore && !reset) return
        if (uiState.answersState.isEnd && !reset) return
        tabJobs[key]?.cancel()
        tabJobs[key] = viewModelScope.launch {
            val isFirstLoad = !uiState.answersState.hasLoaded
            uiState = uiState.copy(
                answersState = uiState.answersState.copy(
                    isLoading = isFirstLoad || (reset && uiState.answersState.items.isEmpty()),
                    isRefreshing = reset && uiState.answersState.items.isNotEmpty(),
                    isLoadingMore = !reset && !isFirstLoad,
                    errorMessage = null,
                ),
            )
            try {
                val next = if (reset) null else nextUrls[key]
                val page = repository.getAnswers(tokenOrId, uiState.answersSort, next)
                nextUrls[key] = page.nextUrl
                val newItems = if (reset) page.items else uiState.answersState.items + page.items
                uiState = uiState.copy(
                    answersState = uiState.answersState.copy(
                        items = newItems,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isEnd = page.isEnd,
                        hasLoaded = true,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to load answers", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    answersState = uiState.answersState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg,
                    ),
                )
                _effect.send(PeopleEffect.ShowMessage("加载回答失败: $msg"))
            }
        }
    }

    fun loadArticles(reset: Boolean) {
        val key = "articles"
        if (uiState.articlesState.isLoadingMore && !reset) return
        if (uiState.articlesState.isEnd && !reset) return
        tabJobs[key]?.cancel()
        tabJobs[key] = viewModelScope.launch {
            val isFirstLoad = !uiState.articlesState.hasLoaded
            uiState = uiState.copy(
                articlesState = uiState.articlesState.copy(
                    isLoading = isFirstLoad || (reset && uiState.articlesState.items.isEmpty()),
                    isRefreshing = reset && uiState.articlesState.items.isNotEmpty(),
                    isLoadingMore = !reset && !isFirstLoad,
                    errorMessage = null,
                ),
            )
            try {
                val next = if (reset) null else nextUrls[key]
                val page = repository.getArticles(tokenOrId, uiState.articlesSort, next)
                nextUrls[key] = page.nextUrl
                val newItems = if (reset) page.items else uiState.articlesState.items + page.items
                uiState = uiState.copy(
                    articlesState = uiState.articlesState.copy(
                        items = newItems,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isEnd = page.isEnd,
                        hasLoaded = true,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to load articles", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    articlesState = uiState.articlesState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg,
                    ),
                )
                _effect.send(PeopleEffect.ShowMessage("加载文章失败: $msg"))
            }
        }
    }

    fun loadActivities(reset: Boolean) {
        val key = "activities"
        if (uiState.activitiesState.isLoadingMore && !reset) return
        if (uiState.activitiesState.isEnd && !reset) return
        tabJobs[key]?.cancel()
        tabJobs[key] = viewModelScope.launch {
            val isFirstLoad = !uiState.activitiesState.hasLoaded
            uiState = uiState.copy(
                activitiesState = uiState.activitiesState.copy(
                    isLoading = isFirstLoad || (reset && uiState.activitiesState.items.isEmpty()),
                    isRefreshing = reset && uiState.activitiesState.items.isNotEmpty(),
                    isLoadingMore = !reset && !isFirstLoad,
                    errorMessage = null,
                ),
            )
            try {
                val next = if (reset) null else nextUrls[key]
                val page = repository.getActivities(tokenOrId, next)
                nextUrls[key] = page.nextUrl
                val newItems = if (reset) page.items else uiState.activitiesState.items + page.items
                uiState = uiState.copy(
                    activitiesState = uiState.activitiesState.copy(
                        items = newItems,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isEnd = page.isEnd,
                        hasLoaded = true,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to load activities", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    activitiesState = uiState.activitiesState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg,
                    ),
                )
                _effect.send(PeopleEffect.ShowMessage("加载动态失败: $msg"))
            }
        }
    }

    fun loadPins(reset: Boolean) {
        val key = "pins"
        if (uiState.pinsState.isLoadingMore && !reset) return
        if (uiState.pinsState.isEnd && !reset) return
        tabJobs[key]?.cancel()
        tabJobs[key] = viewModelScope.launch {
            val isFirstLoad = !uiState.pinsState.hasLoaded
            uiState = uiState.copy(
                pinsState = uiState.pinsState.copy(
                    isLoading = isFirstLoad || (reset && uiState.pinsState.items.isEmpty()),
                    isRefreshing = reset && uiState.pinsState.items.isNotEmpty(),
                    isLoadingMore = !reset && !isFirstLoad,
                    errorMessage = null,
                ),
            )
            try {
                val next = if (reset) null else nextUrls[key]
                val page = repository.getPins(tokenOrId, next)
                nextUrls[key] = page.nextUrl
                val newItems = if (reset) page.items else uiState.pinsState.items + page.items
                uiState = uiState.copy(
                    pinsState = uiState.pinsState.copy(
                        items = newItems,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isEnd = page.isEnd,
                        hasLoaded = true,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to load pins", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    pinsState = uiState.pinsState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg,
                    ),
                )
                _effect.send(PeopleEffect.ShowMessage("加载想法失败: $msg"))
            }
        }
    }

    fun loadCollections(reset: Boolean) {
        val key = "collections"
        if (uiState.collectionsState.isLoadingMore && !reset) return
        if (uiState.collectionsState.isEnd && !reset) return
        tabJobs[key]?.cancel()
        tabJobs[key] = viewModelScope.launch {
            val isFirstLoad = !uiState.collectionsState.hasLoaded
            uiState = uiState.copy(
                collectionsState = uiState.collectionsState.copy(
                    isLoading = isFirstLoad || (reset && uiState.collectionsState.items.isEmpty()),
                    isRefreshing = reset && uiState.collectionsState.items.isNotEmpty(),
                    isLoadingMore = !reset && !isFirstLoad,
                    errorMessage = null,
                ),
            )
            try {
                val next = if (reset) null else nextUrls[key]
                val page = repository.getCollections(tokenOrId, next)
                nextUrls[key] = page.nextUrl
                val newItems = if (reset) page.items else uiState.collectionsState.items + page.items
                uiState = uiState.copy(
                    collectionsState = uiState.collectionsState.copy(
                        items = newItems,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isEnd = page.isEnd,
                        hasLoaded = true,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to load collections", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    collectionsState = uiState.collectionsState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg,
                    ),
                )
                _effect.send(PeopleEffect.ShowMessage("加载收藏失败: $msg"))
            }
        }
    }

    fun loadQuestions(reset: Boolean) {
        val key = "questions"
        if (uiState.questionsState.isLoadingMore && !reset) return
        if (uiState.questionsState.isEnd && !reset) return
        tabJobs[key]?.cancel()
        tabJobs[key] = viewModelScope.launch {
            val isFirstLoad = !uiState.questionsState.hasLoaded
            uiState = uiState.copy(
                questionsState = uiState.questionsState.copy(
                    isLoading = isFirstLoad || (reset && uiState.questionsState.items.isEmpty()),
                    isRefreshing = reset && uiState.questionsState.items.isNotEmpty(),
                    isLoadingMore = !reset && !isFirstLoad,
                    errorMessage = null,
                ),
            )
            try {
                val next = if (reset) null else nextUrls[key]
                val page = repository.getQuestions(tokenOrId, next)
                nextUrls[key] = page.nextUrl
                val newItems = if (reset) page.items else uiState.questionsState.items + page.items
                uiState = uiState.copy(
                    questionsState = uiState.questionsState.copy(
                        items = newItems,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isEnd = page.isEnd,
                        hasLoaded = true,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to load questions", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    questionsState = uiState.questionsState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg,
                    ),
                )
                _effect.send(PeopleEffect.ShowMessage("加载提问失败: $msg"))
            }
        }
    }

    fun loadColumns(reset: Boolean) {
        val key = "columns"
        if (uiState.columnsState.isLoadingMore && !reset) return
        if (uiState.columnsState.isEnd && !reset) return
        tabJobs[key]?.cancel()
        tabJobs[key] = viewModelScope.launch {
            val isFirstLoad = !uiState.columnsState.hasLoaded
            uiState = uiState.copy(
                columnsState = uiState.columnsState.copy(
                    isLoading = isFirstLoad || (reset && uiState.columnsState.items.isEmpty()),
                    isRefreshing = reset && uiState.columnsState.items.isNotEmpty(),
                    isLoadingMore = !reset && !isFirstLoad,
                    errorMessage = null,
                ),
            )
            try {
                val next = if (reset) null else nextUrls[key]
                val page = repository.getColumns(tokenOrId, next)
                nextUrls[key] = page.nextUrl
                val newItems = if (reset) page.items else uiState.columnsState.items + page.items
                uiState = uiState.copy(
                    columnsState = uiState.columnsState.copy(
                        items = newItems,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isEnd = page.isEnd,
                        hasLoaded = true,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to load columns", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    columnsState = uiState.columnsState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg,
                    ),
                )
                _effect.send(PeopleEffect.ShowMessage("加载专栏失败: $msg"))
            }
        }
    }

    fun loadFollowers(reset: Boolean) {
        val key = "followers"
        if (uiState.followersState.isLoadingMore && !reset) return
        if (uiState.followersState.isEnd && !reset) return
        tabJobs[key]?.cancel()
        tabJobs[key] = viewModelScope.launch {
            val isFirstLoad = !uiState.followersState.hasLoaded
            uiState = uiState.copy(
                followersState = uiState.followersState.copy(
                    isLoading = isFirstLoad || (reset && uiState.followersState.items.isEmpty()),
                    isRefreshing = reset && uiState.followersState.items.isNotEmpty(),
                    isLoadingMore = !reset && !isFirstLoad,
                    errorMessage = null,
                ),
            )
            try {
                val next = if (reset) null else nextUrls[key]
                val page = repository.getFollowers(memberId, next)
                nextUrls[key] = page.nextUrl
                val newItems = if (reset) page.items else uiState.followersState.items + page.items
                uiState = uiState.copy(
                    followersState = uiState.followersState.copy(
                        items = newItems,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isEnd = page.isEnd,
                        hasLoaded = true,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to load followers", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    followersState = uiState.followersState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg,
                    ),
                )
                _effect.send(PeopleEffect.ShowMessage("加载粉丝失败: $msg"))
            }
        }
    }

    fun loadFollowing(reset: Boolean) {
        val key = "following"
        if (uiState.followingState.isLoadingMore && !reset) return
        if (uiState.followingState.isEnd && !reset) return
        tabJobs[key]?.cancel()
        tabJobs[key] = viewModelScope.launch {
            val isFirstLoad = !uiState.followingState.hasLoaded
            uiState = uiState.copy(
                followingState = uiState.followingState.copy(
                    isLoading = isFirstLoad || (reset && uiState.followingState.items.isEmpty()),
                    isRefreshing = reset && uiState.followingState.items.isNotEmpty(),
                    isLoadingMore = !reset && !isFirstLoad,
                    errorMessage = null,
                ),
            )
            try {
                val next = if (reset) null else nextUrls[key]
                val page = repository.getFollowing(tokenOrId, next)
                nextUrls[key] = page.nextUrl
                val newItems = if (reset) page.items else uiState.followingState.items + page.items
                uiState = uiState.copy(
                    followingState = uiState.followingState.copy(
                        items = newItems,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isEnd = page.isEnd,
                        hasLoaded = true,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to load following", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    followingState = uiState.followingState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg,
                    ),
                )
                _effect.send(PeopleEffect.ShowMessage("加载关注失败: $msg"))
            }
        }
    }

    fun loadFollowingColumns(reset: Boolean) {
        val key = "sub_columns"
        if (uiState.followingColumnsState.isLoadingMore && !reset) return
        if (uiState.followingColumnsState.isEnd && !reset) return
        tabJobs[key]?.cancel()
        tabJobs[key] = viewModelScope.launch {
            val isFirstLoad = !uiState.followingColumnsState.hasLoaded
            uiState = uiState.copy(
                followingColumnsState = uiState.followingColumnsState.copy(
                    isLoading = isFirstLoad || (reset && uiState.followingColumnsState.items.isEmpty()),
                    isRefreshing = reset && uiState.followingColumnsState.items.isNotEmpty(),
                    isLoadingMore = !reset && !isFirstLoad,
                    errorMessage = null,
                ),
            )
            try {
                val next = if (reset) null else nextUrls[key]
                val page = repository.getFollowingColumns(tokenOrId, next)
                nextUrls[key] = page.nextUrl
                val newItems = if (reset) page.items else uiState.followingColumnsState.items + page.items
                uiState = uiState.copy(
                    followingColumnsState = uiState.followingColumnsState.copy(
                        items = newItems,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isEnd = page.isEnd,
                        hasLoaded = true,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to load following columns", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    followingColumnsState = uiState.followingColumnsState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg,
                    ),
                )
                _effect.send(PeopleEffect.ShowMessage("加载订阅专栏失败: $msg"))
            }
        }
    }

    fun loadFollowingTopics(reset: Boolean) {
        val key = "sub_topics"
        if (uiState.followingTopicsState.isLoadingMore && !reset) return
        if (uiState.followingTopicsState.isEnd && !reset) return
        tabJobs[key]?.cancel()
        tabJobs[key] = viewModelScope.launch {
            val isFirstLoad = !uiState.followingTopicsState.hasLoaded
            uiState = uiState.copy(
                followingTopicsState = uiState.followingTopicsState.copy(
                    isLoading = isFirstLoad || (reset && uiState.followingTopicsState.items.isEmpty()),
                    isRefreshing = reset && uiState.followingTopicsState.items.isNotEmpty(),
                    isLoadingMore = !reset && !isFirstLoad,
                    errorMessage = null,
                ),
            )
            try {
                val next = if (reset) null else nextUrls[key]
                val page = repository.getFollowingTopics(tokenOrId, next)
                nextUrls[key] = page.nextUrl
                val newItems = if (reset) page.items else uiState.followingTopicsState.items + page.items
                uiState = uiState.copy(
                    followingTopicsState = uiState.followingTopicsState.copy(
                        items = newItems,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isEnd = page.isEnd,
                        hasLoaded = true,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to load following topics", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    followingTopicsState = uiState.followingTopicsState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg,
                    ),
                )
                _effect.send(PeopleEffect.ShowMessage("加载关注话题失败: $msg"))
            }
        }
    }

    fun loadFollowingQuestions(reset: Boolean) {
        val key = "sub_questions"
        if (uiState.followingQuestionsState.isLoadingMore && !reset) return
        if (uiState.followingQuestionsState.isEnd && !reset) return
        tabJobs[key]?.cancel()
        tabJobs[key] = viewModelScope.launch {
            val isFirstLoad = !uiState.followingQuestionsState.hasLoaded
            uiState = uiState.copy(
                followingQuestionsState = uiState.followingQuestionsState.copy(
                    isLoading = isFirstLoad || (reset && uiState.followingQuestionsState.items.isEmpty()),
                    isRefreshing = reset && uiState.followingQuestionsState.items.isNotEmpty(),
                    isLoadingMore = !reset && !isFirstLoad,
                    errorMessage = null,
                ),
            )
            try {
                val next = if (reset) null else nextUrls[key]
                val page = repository.getFollowingQuestions(tokenOrId, next)
                nextUrls[key] = page.nextUrl
                val newItems = if (reset) page.items else uiState.followingQuestionsState.items + page.items
                uiState = uiState.copy(
                    followingQuestionsState = uiState.followingQuestionsState.copy(
                        items = newItems,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isEnd = page.isEnd,
                        hasLoaded = true,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to load following questions", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    followingQuestionsState = uiState.followingQuestionsState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg,
                    ),
                )
                _effect.send(PeopleEffect.ShowMessage("加载关注问题失败: $msg"))
            }
        }
    }

    fun loadFollowingCollections(reset: Boolean) {
        val key = "sub_collections"
        if (uiState.followingCollectionsState.isLoadingMore && !reset) return
        if (uiState.followingCollectionsState.isEnd && !reset) return
        tabJobs[key]?.cancel()
        tabJobs[key] = viewModelScope.launch {
            val isFirstLoad = !uiState.followingCollectionsState.hasLoaded
            uiState = uiState.copy(
                followingCollectionsState = uiState.followingCollectionsState.copy(
                    isLoading = isFirstLoad || (reset && uiState.followingCollectionsState.items.isEmpty()),
                    isRefreshing = reset && uiState.followingCollectionsState.items.isNotEmpty(),
                    isLoadingMore = !reset && !isFirstLoad,
                    errorMessage = null,
                ),
            )
            try {
                val next = if (reset) null else nextUrls[key]
                val page = repository.getFollowingCollections(tokenOrId, next)
                nextUrls[key] = page.nextUrl
                val newItems = if (reset) page.items else uiState.followingCollectionsState.items + page.items
                uiState = uiState.copy(
                    followingCollectionsState = uiState.followingCollectionsState.copy(
                        items = newItems,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isEnd = page.isEnd,
                        hasLoaded = true,
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("PeopleViewModel", "Failed to load following collections", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    followingCollectionsState = uiState.followingCollectionsState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg,
                    ),
                )
                _effect.send(PeopleEffect.ShowMessage("加载关注收藏夹失败: $msg"))
            }
        }
    }
}
