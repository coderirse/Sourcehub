package com.example.sourcehub.domain.model

data class Download(
    val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val productId: String = "",
    val fileName: String = "",
    val fileUrl: String = "",
    val localPath: String = "",
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val fileType: FileType = FileType.PDF,
    val createdAt: Long = System.currentTimeMillis()
)

enum class DownloadStatus(val label: String) {
    PENDING("等待下载"),
    DOWNLOADING("下载中"),
    PAUSED("已暂停"),
    COMPLETED("已完成"),
    FAILED("下载失败")
}
