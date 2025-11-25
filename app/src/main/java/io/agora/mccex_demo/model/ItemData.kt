package io.agora.mccex_demo.model

enum class DownloadState {
    IDLE,           // 未下载
    WAITING,        // 等待下载
    DOWNLOADING,    // 下载中
    COMPLETED,      // 已完成
    FAILED          // 下载失败
}

data class ItemData(
    val content: String,
    var bgResId: Int = 0,
    var success: Boolean = false,
    val nextTestCaseIndex: Int = -1,
    var isPreloaded: Boolean = false,   // 是否已下载完成
    var downloadProgress: Int = 0,       // 下载进度 0-100
    var isPlaying: Boolean = false,      // 是否正在播放
    var downloadState: DownloadState = DownloadState.IDLE  // 下载状态
)
