@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.mediajournal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val contents: List<TrackedContent> = emptyList(),
    val inProgressContents: List<TrackedContent> = emptyList(),
    val selectedType: ContentType? = null,
    val selectedStatus: ContentStatus? = null,
    val searchQuery: String = "",
    val summary: HomeSummary = HomeSummary()
)

data class HomeSummary(
    val inProgress: Int = 0,
    val finishedThisMonth: Int = 0,
    val pending: Int = 0
)

class HomeViewModel(
    private val repository: ContentRepository
) : ViewModel() {
    private val selectedType = MutableStateFlow<ContentType?>(null)
    private val selectedStatus = MutableStateFlow<ContentStatus?>(null)
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeContents(),
        selectedType,
        selectedStatus,
        searchQuery
    ) { contents, type, status, query ->
        val today = LocalDate.now()
        val filtered = contents
            .filter { type == null || it.type == type }
            .filter { status == null || it.status == status }
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
        val inProgress = contents
            .filter { it.status == ContentStatus.IN_PROGRESS }
            .filter { type == null || it.type == type }
            .filter { status == null || status == ContentStatus.IN_PROGRESS }
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }

        HomeUiState(
            contents = filtered,
            inProgressContents = inProgress,
            selectedType = type,
            selectedStatus = status,
            searchQuery = query,
            summary = HomeSummary(
                inProgress = contents.count { it.status == ContentStatus.IN_PROGRESS },
                finishedThisMonth = contents.count {
                    val finishedDate = it.finishedDate
                    it.status == ContentStatus.FINISHED &&
                        finishedDate != null &&
                        finishedDate.year == today.year &&
                        finishedDate.monthValue == today.monthValue
                },
                pending = contents.count { it.status == ContentStatus.PENDING }
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setType(type: ContentType?) {
        selectedType.value = type
    }

    fun setStatus(status: ContentStatus?) {
        selectedStatus.value = status
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun markFinished(content: TrackedContent) {
        val today = LocalDate.now()
        viewModelScope.launch {
            repository.save(
                content.copy(
                    status = ContentStatus.FINISHED,
                    startDate = content.startDate ?: today,
                    finishedDate = today
                )
            )
        }
    }
}

data class DetailUiState(
    val content: TrackedContent? = null,
    val deleted: Boolean = false
)

class DetailViewModel(
    private val repository: ContentRepository,
    contentId: Long
) : ViewModel() {
    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeContent(contentId),
        deleted
    ) { content, wasDeleted ->
        DetailUiState(content = content, deleted = wasDeleted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun delete() {
        val content = uiState.value.content ?: return
        viewModelScope.launch {
            repository.delete(content)
            deleted.value = true
        }
    }

    fun updateStatus(status: ContentStatus) {
        val content = uiState.value.content ?: return
        val today = LocalDate.now()
        viewModelScope.launch {
            repository.save(
                content.copy(
                    status = status,
                    startDate = content.startDate ?: today.takeIf { status != ContentStatus.PENDING },
                    finishedDate = today.takeIf { status == ContentStatus.FINISHED }
                )
            )
        }
    }
}

data class EditUiState(
    val id: Long = 0,
    val title: String = "",
    val type: ContentType = ContentType.SERIES,
    val isTypeLocked: Boolean = false,
    val status: ContentStatus = ContentStatus.PENDING,
    val rating: Int? = null,
    val genre: String = "",
    val coverUrl: String = "",
    val notes: String = "",
    val saved: Boolean = false,
    val titleError: Boolean = false
)

class EditContentViewModel(
    private val repository: ContentRepository,
    private val contentId: Long?,
    initialType: ContentType? = null
) : ViewModel() {
    private val state = MutableStateFlow(
        EditUiState(
            type = initialType ?: ContentType.SERIES,
            isTypeLocked = initialType != null
        )
    )
    val uiState: StateFlow<EditUiState> = state

    init {
        if (contentId != null) {
            viewModelScope.launch {
                repository.observeContent(contentId)
                    .filterNotNull()
                    .collect { content ->
                        state.value = EditUiState(
                            id = content.id,
                            title = content.title,
                            type = content.type,
                            isTypeLocked = false,
                            status = content.status,
                            rating = content.rating,
                            genre = content.genre.orEmpty(),
                            coverUrl = content.coverUrl.orEmpty(),
                            notes = content.notes.orEmpty()
                        )
                    }
            }
        }
    }

    fun updateTitle(value: String) = state.update { it.copy(title = value, titleError = false) }
    fun updateType(value: ContentType) = state.update {
        it.copy(
            type = value,
            genre = ""
        )
    }
    fun updateStatus(value: ContentStatus) = state.update { it.copy(status = value) }
    fun updateRating(value: Int?) = state.update { it.copy(rating = value) }
    fun updateGenre(value: String) = state.update { it.copy(genre = value) }
    fun updateCoverUrl(value: String) = state.update { it.copy(coverUrl = value) }
    fun updateNotes(value: String) = state.update { it.copy(notes = value) }

    fun save() {
        val current = state.value
        if (current.title.isBlank()) {
            state.update { it.copy(titleError = true) }
            return
        }

        viewModelScope.launch {
            val today = LocalDate.now()
            repository.save(
                TrackedContent(
                    id = current.id,
                    title = current.title.trim(),
                    type = current.type,
                    status = current.status,
                    rating = current.rating,
                    genre = current.genre.takeIf { it.isNotBlank() },
                    coverUrl = current.coverUrl.takeIf { it.isNotBlank() },
                    notes = current.notes.takeIf { it.isNotBlank() },
                    startDate = today.takeIf { current.status != ContentStatus.PENDING },
                    finishedDate = today.takeIf { current.status == ContentStatus.FINISHED },
                    currentProgress = null,
                    totalProgress = null
                )
            )
            state.update { it.copy(saved = true) }
        }
    }
}

data class StatsUiState(
    val selectedYear: Int = LocalDate.now().year,
    val selectedMonth: Int = LocalDate.now().monthValue,
    val stats: MonthlyStats? = null
)

class StatsViewModel(
    private val repository: ContentRepository
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now().withDayOfMonth(1))

    val uiState: StateFlow<StatsUiState> = selectedDate
        .flatMapLatest { date -> repository.observeMonthlyStats(date.year, date.monthValue) }
        .combine(selectedDate) { stats, date ->
            StatsUiState(date.year, date.monthValue, stats)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun previousMonth() {
        selectedDate.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        selectedDate.update { it.plusMonths(1) }
    }
}

data class HistoricalUiState(
    val stats: HistoricalStats? = null
)

class HistoricalViewModel(
    repository: ContentRepository
) : ViewModel() {
    val uiState: StateFlow<HistoricalUiState> = repository.observeHistoricalStats()
        .combine(MutableStateFlow(Unit)) { stats, _ -> HistoricalUiState(stats) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoricalUiState())
}

class RepositoryViewModelFactory<T : ViewModel>(
    private val create: () -> T
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
