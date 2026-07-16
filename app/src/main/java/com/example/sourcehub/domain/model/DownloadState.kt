package com.example.sourcehub.domain.model

/**
 * 单个[Download]任务的实时下载进度快照。
 *
 * 与表示持久化任务记录的[Download]不同，此数据类
 * 携带由[DownloadWorker]频繁更新并在界面中观察的
 * 瞬时进度数据（已接收字节数、速度）。
 *
 * @property downloadId 此状态所属的下载任务。
 * @property progress 完成比例（0.0 - 1.0），由 downloadedBytes / totalBytes 计算得出。
 * @property downloadedBytes 累计已接收的字节数。
 * @property totalBytes 预期文件总大小（字节）。
 * @property speed 估算下载速度（字节/秒）。
 * @property status 当前生命周期状态，实时更新。
 */
data class DownloadState(
    val downloadId: String = "",
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speed: Long = 0L, // 字节/秒
    val status: DownloadStatus = DownloadStatus.PENDING
)
