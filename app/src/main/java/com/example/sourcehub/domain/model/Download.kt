package com.example.sourcehub.domain.model

/**
 * 表示文件下载任务的领域模型。
 *
 * 支付成功后用户可以下载已购买的数字商品。每个[Download]
 * 跟踪下载生命周期、进度和本地存储路径。它持久化在
 * SQLite数据库中，并通过[DownloadRepository]在界面中展示。
 *
 * @property id 唯一下载任务标识符（例如 "dl_a1b2c3d4"）。
 * @property userId 拥有此下载的用户。
 * @property orderId 授权此下载的已完成订单。
 * @property productId 正在下载的商品。
 * @property fileName 包含扩展名的文件名（例如 "Python编程.pdf"）。
 * @property fileUrl 要下载文件的远程 URL。
 * @property localPath 下载完成后设备上的绝对路径。
 * @property fileSize 文件总大小（字节）；用于进度计算。
 * @property downloadedBytes 累计已接收的字节数。
 * @property status 当前下载生命周期状态。
 * @property fileType 文件格式，用于选择合适的打开应用。
 * @property createdAt 下载任务创建的毫秒时间戳。
 */
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

/**
 * [Download]任务的生命周期状态。
 *
 * 流程为：等待下载 -> 下载中 -> 已完成（或下载失败）。
 * 已暂停从下载中进入，恢复后回到下载中。
 *
 * @property label 用于界面渲染的中文本地化显示标签。
 */
enum class DownloadStatus(val label: String) {
    PENDING("等待下载"),
    DOWNLOADING("下载中"),
    PAUSED("已暂停"),
    COMPLETED("已完成"),
    FAILED("下载失败")
}
