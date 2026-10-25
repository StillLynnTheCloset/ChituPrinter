package com.stilllynnthecloset.chituprinter

import androidx.lifecycle.ViewModel

class PlayerViewModel : ViewModel() {
//
//    private val _playerState = MutableStateFlow<ExoPlayer?>(null)
//    val playerState: StateFlow<ExoPlayer?> = _playerState
//
//    private var currentPosition: Long = 0L
//
//    fun initializePlayer(context: Context, videoUrl: String) {
//        if (_playerState.value == null) {
//            viewModelScope.launch {
//                val exoPlayer = ExoPlayer.Builder(context).build().also {
//                    val mediaItem = MediaItem.fromUri(videoUrl.toUri())
//                    it.setMediaItem(mediaItem)
//                    it.prepare()
//                    it.playWhenReady = true
////                    it.seekTo(currentPosition)
//                    it.addListener(object : Player.Listener {
//                        override fun onPlayerError(error: PlaybackException) {
//                            handleError(error)
//                        }
//                    })
//                }
//                _playerState.value = exoPlayer
//            }
//        }
//    }
//
//    fun savePlayerState() {
//        _playerState.value?.let {
//            currentPosition = it.currentPosition
//        }
//    }
//
//    fun releasePlayer() {
//        _playerState.value?.release()
//        _playerState.value = null
//    }
//
//    private fun handleError(error: PlaybackException) {
//        when (error.errorCode) {
//            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
//                // Handle network connection error
//                println("Network connection error")
//            }
//
//            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> {
//                // Handle file not found error
//                println("File not found")
//            }
//
//            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> {
//                // Handle decoder initialization error
//                println("Decoder initialization error")
//            }
//
//            else -> {
//                // Handle other types of errors
//                println("Other error: ${error.message}")
//            }
//        }
//    }
}