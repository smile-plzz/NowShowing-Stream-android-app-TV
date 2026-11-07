package com.nowshowing.tv.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nowshowing.tv.data.model.OmdbEpisode
import com.nowshowing.tv.data.model.OmdbTitleDetail
import com.nowshowing.tv.data.repository.MediaRepository
import com.nowshowing.tv.domain.source.SourceRegistry
import com.nowshowing.tv.domain.source.VideoSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class DetailsUiState(
    val imdbId: String = "",
    val type: String = "movie",
    val isLoading: Boolean = true,
    val detail: OmdbTitleDetail? = null,
    val isInWatchlist: Boolean = false,
    val isWatched: Boolean = false,
    val watchedEpisodes: Set<Pair<Int, Int>> = emptySet(),
    val totalSeasons: Int = 1,
    val selectedSeason: Int = 1,
    val episodes: List<OmdbEpisode> = emptyList(),
    val episodesLoading: Boolean = false,
    val episodeError: String? = null,
    val selectedEpisode: Int = 1,
    val selectedSource: VideoSource = SourceRegistry.SOURCES.first(),
    val recommendations: List<OmdbTitleDetail> = emptyList()
)

class DetailsViewModel(application: Application, private val repository: MediaRepository) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, MediaRepository(application))

    private var loadJob: Job? = null
    private var seasonJob: Job? = null
    private var watchlistJob: Job? = null
    private var watchedJob: Job? = null
    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    fun loadDetails(imdbId: String, type: String) {
        if (_uiState.value.imdbId == imdbId && _uiState.value.detail != null) {
            _uiState.value = _uiState.value.copy(selectedSource = SourceRegistry.getSourceForTitle(getApplication(), imdbId))
            viewModelScope.launch {
                val saved = repository.isInWatchlist(imdbId)
                val watched = repository.getRecentlyOpened().firstOrNull { it.imdbID == imdbId }?.watched ?: false
                val watchedEps = repository.getWatchedEpisodes(imdbId)
                if (_uiState.value.imdbId == imdbId) {
                    _uiState.value = _uiState.value.copy(
                        isInWatchlist = saved,
                        isWatched = watched,
                        watchedEpisodes = watchedEps
                    )
                }
            }
            return
        }
        loadJob?.cancel()
        seasonJob?.cancel()
        _uiState.value = DetailsUiState(
            imdbId = imdbId,
            type = type,
            isLoading = true,
            selectedSource = SourceRegistry.getSourceForTitle(getApplication(), imdbId)
        )
        loadJob = viewModelScope.launch {
            val inWatchlist = repository.isInWatchlist(imdbId)
            val watched = repository.getRecentlyOpened().firstOrNull { it.imdbID == imdbId }?.watched ?: false
            val watchedEps = repository.getWatchedEpisodes(imdbId)
            _uiState.value = _uiState.value.copy(
                isInWatchlist = inWatchlist,
                isWatched = watched,
                watchedEpisodes = watchedEps
            )
            val detail = repository.getDetails(imdbId)
            if (detail != null) {
                val isSeries = detail.type == "series" || type == "series"
                val numSeasons = detail.totalSeasons?.toIntOrNull()?.coerceAtLeast(1) ?: 1

                _uiState.value = _uiState.value.copy(
                    detail = detail,
                    type = if (isSeries) "series" else "movie",
                    totalSeasons = numSeasons
                )

                // If series, load remembered episode when available
                val lastOpened = repository.getRecentlyOpened().firstOrNull { it.imdbID == imdbId }
                val savedSeason = lastOpened?.season?.coerceIn(1, numSeasons.coerceAtLeast(1)) ?: 1
                _uiState.value = _uiState.value.copy(
                    selectedSeason = savedSeason,
                    selectedEpisode = lastOpened?.episode?.coerceAtLeast(1) ?: 1,
                    isLoading = false
                )
                // If series, load season episodes
                if (isSeries) {
                    loadSeasonEpisodes(imdbId, savedSeason)
                }

                // Reuse genre-matched cached candidates; no title-word search or extra network calls.
                val recommendations = repository.relatedTitles(detail)
                _uiState.value = _uiState.value.copy(recommendations = recommendations)

                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectSeason(season: Int) {
        if (_uiState.value.type != "series" || season !in 1.._uiState.value.totalSeasons) return
        _uiState.value = _uiState.value.copy(selectedSeason = season, selectedEpisode = 1, episodes = emptyList())
        loadSeasonEpisodes(_uiState.value.imdbId, season)
    }

    fun selectEpisode(episode: Int) {
        if (episode < 1) return
        _uiState.value = _uiState.value.copy(selectedEpisode = episode)
    }

    fun retryEpisodes() {
        com.nowshowing.tv.data.repository.MetadataHealth.retry()
        loadSeasonEpisodes(_uiState.value.imdbId, _uiState.value.selectedSeason)
    }

    private fun loadSeasonEpisodes(imdbId: String, season: Int) {
        seasonJob?.cancel()
        _uiState.value = _uiState.value.copy(episodesLoading = true, episodeError = null)
        seasonJob = viewModelScope.launch {
            val seasonData = repository.getSeason(imdbId, season)
            if (_uiState.value.imdbId != imdbId || _uiState.value.selectedSeason != season) return@launch
            val eps = seasonData?.episodes.orEmpty().filter { (it.episode.toIntOrNull() ?: 0) > 0 }
                .distinctBy { it.episode.toInt() }.sortedBy { it.episode.toInt() }
            val selected = _uiState.value.selectedEpisode
            _uiState.value = _uiState.value.copy(
                episodes = eps, episodesLoading = false,
                selectedEpisode = if (eps.isEmpty() || eps.any { it.episode.toInt() == selected }) selected else eps.first().episode.toInt(),
                episodeError = if (eps.isEmpty()) "Episode list unavailable for season $season. Retry to load it." else null
            )
        }
    }

    fun toggleWatchlist() {
        val detail = _uiState.value.detail ?: return
        if (watchlistJob?.isActive == true) return
        watchlistJob = viewModelScope.launch {
            val isNowInList = repository.toggleWatchlist(detail)
            if (_uiState.value.imdbId == detail.imdbID) _uiState.value = _uiState.value.copy(isInWatchlist = isNowInList)
        }
    }

    fun toggleTitleWatched() {
        val id = _uiState.value.imdbId
        if (id.isBlank() || watchedJob?.isActive == true) return
        val newWatched = !_uiState.value.isWatched
        watchedJob = viewModelScope.launch {
            repository.setRecentlyWatched(id, newWatched)
            if (_uiState.value.imdbId == id) {
                _uiState.value = _uiState.value.copy(isWatched = newWatched)
            }
        }
    }

    fun toggleEpisodeWatched(season: Int, episode: Int) {
        val id = _uiState.value.imdbId
        if (id.isBlank()) return
        val pair = Pair(season, episode)
        val isCurrentlyWatched = _uiState.value.watchedEpisodes.contains(pair)
        viewModelScope.launch {
            repository.setEpisodeWatched(id, season, episode, !isCurrentlyWatched)
            val updated = repository.getWatchedEpisodes(id)
            if (_uiState.value.imdbId == id) {
                _uiState.value = _uiState.value.copy(watchedEpisodes = updated)
            }
        }
    }

    fun markPreviousEpisodesWatched(
        season: Int = _uiState.value.selectedSeason,
        episode: Int = _uiState.value.selectedEpisode
    ) {
        val id = _uiState.value.imdbId
        if (id.isBlank()) return
        viewModelScope.launch {
            repository.markPreviousEpisodesWatched(id, season, episode)
            val updated = repository.getWatchedEpisodes(id)
            if (_uiState.value.imdbId == id) {
                _uiState.value = _uiState.value.copy(watchedEpisodes = updated)
            }
        }
    }

    fun setSource(source: VideoSource) {
        _uiState.value = _uiState.value.copy(selectedSource = source)
        SourceRegistry.setPreferredSource(getApplication(), source.name)
        SourceRegistry.setSourceForTitle(getApplication(), _uiState.value.imdbId, source)
    }
}
