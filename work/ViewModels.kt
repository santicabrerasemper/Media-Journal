@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.mediajournal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val contents: List<TrackedContent> = emptyList(),
    val inProgressContents: List<TrackedContent> = emptyList(),
    val availableGenres: List<String> = emptyList(),
    val selectedType: ContentType? = null,
    val selectedStatus: ContentStatus? = null,
    val selectedGenre: String? = null,
    val selectedSort: HomeSort = HomeSort.RECENT,
    val searchQuery: String = "",
    val summary: HomeSummary = HomeSummary()
)

enum class HomeSort(val label: String) {
    RECENT("Recientes"),
    BEST_RATED("Mejor calificados"),
    PENDING_FIRST("Pendientes primero"),
    FINISHED_FIRST("Terminados primero")
}

data class HomeSummary(
    val inProgress: Int = 0,
    val finishedThisMonth: Int = 0,
    val pending: Int = 0
)

private data class HomeFilters(
    val type: ContentType?,
    val status: ContentStatus?,
    val genre: String?,
    val sort: HomeSort
)

class HomeViewModel(
    private val repository: ContentRepository
) : ViewModel() {
    private val selectedType = MutableStateFlow<ContentType?>(null)
    private val selectedStatus = MutableStateFlow<ContentStatus?>(null)
    private val selectedGenre = MutableStateFlow<String?>(null)
    private val selectedSort = MutableStateFlow(HomeSort.RECENT)
    private val searchQuery = MutableStateFlow("")
    private val filters = combine(
        selectedType,
        selectedStatus,
        selectedGenre,
        selectedSort
    ) { type, status, genre, sort ->
        HomeFilters(type = type, status = status, genre = genre, sort = sort)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeContents(),
        filters,
        searchQuery
    ) { contents, filters, query ->
        val type = filters.type
        val status = filters.status
        val genre = filters.genre
        val sort = filters.sort
        val today = LocalDate.now()
        val availableGenres = contents
            .filter { type == null || it.type == type }
            .flatMap { it.genre.orEmpty().split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val effectiveGenre = genre.takeIf { it in availableGenres }
        val filtered = contents
            .filter { type == null || it.type == type }
            .filter { status == null || it.status == status }
            .filter { effectiveGenre == null || hasGenre(it, effectiveGenre) }
            .filter { query.isBlank() || matchesSearch(it, query) }
            .sortedWith(sort.comparator())
        val inProgress = contents
            .filter { it.status == ContentStatus.IN_PROGRESS }
            .filter { type == null || it.type == type }
            .filter { status == null || status == ContentStatus.IN_PROGRESS }
            .filter { effectiveGenre == null || hasGenre(it, effectiveGenre) }
            .filter { query.isBlank() || matchesSearch(it, query) }
            .sortedWith(HomeSort.RECENT.comparator())

        HomeUiState(
            contents = filtered,
            inProgressContents = inProgress,
            availableGenres = availableGenres,
            selectedType = type,
            selectedStatus = status,
            selectedGenre = effectiveGenre,
            selectedSort = sort,
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

    fun setGenre(genre: String?) {
        selectedGenre.value = genre
    }

    fun setSort(sort: HomeSort) {
        selectedSort.value = sort
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

    fun updateStatus(content: TrackedContent, status: ContentStatus) {
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

    fun updateRating(content: TrackedContent, rating: Int?) {
        viewModelScope.launch {
            repository.save(content.copy(rating = rating))
        }
    }
}

private fun hasGenre(content: TrackedContent, genre: String): Boolean {
    return content.genre.orEmpty()
        .split(",")
        .map { it.trim() }
        .any { it.equals(genre, ignoreCase = true) }
}

private fun matchesSearch(content: TrackedContent, query: String): Boolean {
    return content.title.contains(query, ignoreCase = true) ||
        content.genre.orEmpty().contains(query, ignoreCase = true) ||
        content.notes.orEmpty().contains(query, ignoreCase = true) ||
        content.type.label.contains(query, ignoreCase = true)
}

private fun HomeSort.comparator(): Comparator<TrackedContent> {
    return when (this) {
        HomeSort.RECENT -> compareByDescending<TrackedContent> { it.updatedAt }
        HomeSort.BEST_RATED -> compareByDescending<TrackedContent> { it.rating ?: -1 }
            .thenByDescending { it.updatedAt }
        HomeSort.PENDING_FIRST -> compareBy<TrackedContent> { if (it.status == ContentStatus.PENDING) 0 else 1 }
            .thenByDescending { it.updatedAt }
        HomeSort.FINISHED_FIRST -> compareBy<TrackedContent> { if (it.status == ContentStatus.FINISHED) 0 else 1 }
            .thenByDescending { it.updatedAt }
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
    val coverResults: List<CoverSearchResult> = emptyList(),
    val isSearchingCover: Boolean = false,
    val coverSearchError: String? = null,
    val notes: String = "",
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val titleError: Boolean = false,
    val duplicateCandidates: List<TrackedContent> = emptyList()
)

class EditContentViewModel(
    private val repository: ContentRepository,
    private val contentId: Long?,
    initialType: ContentType? = null,
    private val coverSearchRepository: CoverSearchRepository = CoverSearchRepository()
) : ViewModel() {
    private var coverSearchJob: Job? = null
    private var automaticallySelectedCoverUrl: String? = null
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
                val content = repository.observeContent(contentId).filterNotNull().first()
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
                scheduleCoverSuggestions()
            }
        }
    }

    fun updateTitle(value: String) {
        coverSearchJob?.cancel()
        val previousTitle = state.value.title
        val titleChanged = value != previousTitle
        state.update {
            if (titleChanged) automaticallySelectedCoverUrl = null
            it.copy(
                title = value,
                titleError = false,
                coverUrl = if (titleChanged) "" else it.coverUrl,
                coverResults = emptyList(),
                coverSearchError = null,
                isSearchingCover = false
            )
        }
        scheduleCoverSuggestions()
    }
    fun updateType(value: ContentType) {
        automaticallySelectedCoverUrl = null
        state.update {
            it.copy(
                type = value,
                genre = "",
                coverUrl = "",
                coverResults = emptyList(),
                coverSearchError = null
            )
        }
        scheduleCoverSuggestions()
    }
    fun updateStatus(value: ContentStatus) = state.update { it.copy(status = value) }
    fun updateRating(value: Int?) = state.update { it.copy(rating = value) }
    fun updateGenre(value: String) = state.update { it.copy(genre = value) }
    fun updateCoverUrl(value: String) {
        coverSearchJob?.cancel()
        automaticallySelectedCoverUrl = null
        state.update { it.copy(coverUrl = value, coverSearchError = null, coverResults = if (value.isBlank()) it.coverResults else emptyList()) }
    }
    fun updateNotes(value: String) = state.update { it.copy(notes = value) }

    private fun scheduleCoverSuggestions() {
        coverSearchJob?.cancel()
        val current = state.value
        if (current.coverUrl.isNotBlank() || current.title.trim().length < 3) return
        coverSearchJob = viewModelScope.launch {
            delay(1_000)
            val latest = state.value
            if (latest.coverUrl.isBlank() && latest.title.trim().length >= 3) {
                state.update { it.copy(isSearchingCover = true, coverSearchError = null, coverResults = emptyList()) }
                runCoverSearch(
                    query = latest.title.trim(),
                    type = latest.type,
                    showEmptyError = false
                )
            }
        }
    }

    private suspend fun runCoverSearch(query: String, type: ContentType, showEmptyError: Boolean) {
        runCatching {
            coverSearchRepository.searchBroad(query, type)
        }.onSuccess { results ->
            val latest = state.value
            if (!latest.title.trim().equals(query, ignoreCase = true) || latest.type != type || latest.coverUrl.isNotBlank()) {
                state.update { it.copy(isSearchingCover = false) }
                return@onSuccess
            }
            val singleResult = results.singleOrNull()
            automaticallySelectedCoverUrl = singleResult?.imageUrl
            state.update {
                it.copy(
                    isSearchingCover = false,
                    coverUrl = singleResult?.imageUrl.orEmpty(),
                    coverResults = if (singleResult == null) results else emptyList(),
                    coverSearchError = if (showEmptyError && results.isEmpty()) "No encontré portadas para ese título" else null
                )
            }
        }.onFailure {
            state.update {
                it.copy(
                    isSearchingCover = false,
                    coverSearchError = if (showEmptyError) "No pude buscar portadas. Revisá tu conexión." else null
                )
            }
        }
    }

    fun selectCover(result: CoverSearchResult) {
        automaticallySelectedCoverUrl = null
        state.update {
            it.copy(
                coverUrl = result.imageUrl,
                coverResults = emptyList(),
                coverSearchError = null
            )
        }
    }

    fun chooseAnotherCover() {
        coverSearchJob?.cancel()
        automaticallySelectedCoverUrl = null
        val current = state.value
        if (current.title.trim().length < 3) {
            state.update { it.copy(coverUrl = "", coverResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            state.update { it.copy(coverUrl = "", isSearchingCover = true, coverSearchError = null, coverResults = emptyList()) }
            runCatching {
                coverSearchRepository.searchBroad(current.title.trim(), current.type)
            }.onSuccess { results ->
                val latest = state.value
                if (!latest.title.trim().equals(current.title.trim(), ignoreCase = true) || latest.type != current.type) {
                    state.update { it.copy(isSearchingCover = false) }
                    return@onSuccess
                }
                state.update {
                    it.copy(
                        isSearchingCover = false,
                        coverResults = results,
                        coverSearchError = if (results.isEmpty()) "No encontré portadas para ese título" else null
                    )
                }
            }.onFailure {
                state.update {
                    it.copy(
                        isSearchingCover = false,
                        coverSearchError = "No pude buscar portadas. Revisá tu conexión."
                    )
                }
            }
        }
    }

    fun dismissDuplicateWarning() {
        state.update { it.copy(duplicateCandidates = emptyList()) }
    }

    fun confirmDuplicateSave() {
        val current = state.value.copy(duplicateCandidates = emptyList())
        viewModelScope.launch {
            saveContent(current)
        }
    }

    fun save() {
        val current = state.value
        if (current.isSaving) return
        if (current.title.isBlank()) {
            state.update { it.copy(titleError = true) }
            return
        }

        viewModelScope.launch {
            val duplicates = findDuplicates(current)
            if (duplicates.isNotEmpty()) {
                state.update { it.copy(duplicateCandidates = duplicates) }
                return@launch
            }
            saveContent(current)
        }
    }

    private suspend fun findDuplicates(current: EditUiState): List<TrackedContent> {
        val normalizedTitle = current.title.normalizedForMatch()
        if (normalizedTitle.length < 4) return emptyList()
        return repository.observeContents().first()
            .filter { it.id != current.id }
            .filter { it.type == current.type }
            .filter { existing ->
                val existingTitle = existing.title.normalizedForMatch()
                existingTitle == normalizedTitle ||
                    existingTitle.contains(normalizedTitle) ||
                    normalizedTitle.contains(existingTitle) ||
                    existingTitle.wordsOverlap(normalizedTitle)
            }
            .take(3)
    }

    private suspend fun saveContent(current: EditUiState) {
        state.update { it.copy(isSaving = true, duplicateCandidates = emptyList()) }
        val today = LocalDate.now()
        val existing = current.id.takeIf { it != 0L }
            ?.let { repository.observeContent(it).first() }
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
                startDate = existing?.startDate ?: today.takeIf { current.status != ContentStatus.PENDING },
                finishedDate = today.takeIf { current.status == ContentStatus.FINISHED },
                currentProgress = null,
                totalProgress = null,
                createdAt = existing?.createdAt ?: java.time.LocalDateTime.now()
            )
        )
        state.update { it.copy(isSaving = false, saved = true) }
    }
}

private fun String.normalizedForMatch(): String {
    return lowercase()
        .replace(Regex("[^a-z0-9áéíóúüñ ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun String.wordsOverlap(other: String): Boolean {
    val first = split(" ").filter { it.length > 3 }.toSet()
    val second = other.split(" ").filter { it.length > 3 }.toSet()
    if (first.isEmpty() || second.isEmpty()) return false
    return first.intersect(second).size >= 2
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
