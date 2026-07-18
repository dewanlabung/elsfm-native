package com.elsfm.mobile.feature.profile.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.database.dao.DiscoveryCacheDao
import com.elsfm.mobile.core.database.dao.LibraryCacheDao
import com.elsfm.mobile.core.database.dao.ProfileCacheDao
import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val discoveryCacheDao: DiscoveryCacheDao,
    private val libraryCacheDao: LibraryCacheDao,
    private val profileCacheDao: ProfileCacheDao,
    private val downloadedTrackDao: DownloadedTrackDao,
) : ViewModel() {

    private val _state = MutableStateFlow(StorageState())
    val state: StateFlow<StorageState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val discovery = discoveryCacheDao.getSizeBytes() ?: 0L
            val library = libraryCacheDao.getSizeBytes() ?: 0L
            val profile = profileCacheDao.getSizeBytes() ?: 0L
            val downloads = downloadedTrackDao.getTotalSizeBytes().first() ?: 0L
            _state.value = StorageState(
                discoveryCacheBytes = discovery,
                libraryCacheBytes = library,
                profileCacheBytes = profile,
                downloadedTracksBytes = downloads,
            )
        }
    }

    fun toggleExpanded() {
        _state.value = _state.value.copy(isExpanded = !_state.value.isExpanded)
    }

    fun clearAllCaches() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isClearing = true)
            discoveryCacheDao.clear()
            libraryCacheDao.clear()
            profileCacheDao.clear()
            _state.value = _state.value.copy(
                discoveryCacheBytes = 0L,
                libraryCacheBytes = 0L,
                profileCacheBytes = 0L,
                isClearing = false,
            )
        }
    }
}
