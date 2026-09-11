package com.smile.karaoke.ui_states

import com.smile.karaoke.models.FileDescription

sealed interface OpenFileUiState {
    companion object {
        const val EXCESS_MAX = 1
        const val NO_FILES_SELECTED = 2
        const val ADD_TO_FAVORITES = 3
    }
    object Initial: OpenFileUiState
    object StartLoading: OpenFileUiState
    object StopLoading: OpenFileUiState
    data class FinishLoading(val fileList: List<FileDescription>?, val timestamp: Long = System.nanoTime()): OpenFileUiState
    data class ShowToast(val event: Int, val timestamp: Long = System.nanoTime()): OpenFileUiState
    data class UpdateSelectedSong(val position: Int, val timestamp: Long = System.nanoTime()): OpenFileUiState
}