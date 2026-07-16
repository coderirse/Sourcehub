package com.example.sourcehub.domain.model

/**
 * 表示可购买数字商品的领域模型。
 *
 * 商品是市场的核心实体。每个商品是在某个分类下列出的可下载数字文件
 * （PDF、DOCX、PPTX 等），包含定价、评分和销售元数据。
 * 商品在浏览流程中展示，并通过订单进行购买。
 *
 * @property id 唯一商品标识符（例如 "prod_1"）。
 * @property title 在列表和详情页中展示的人类可读商品名称。
 * @property description 在商品详情页面显示的全文本描述。
 * @property author 创作者或发布者名称。
 * @property price 以本地货币单位计算的当前售价。
 * @property originalPrice 原始标价，用于为折扣显示"原价"。
 * @property coverUrl 商品封面/缩略图的远程 URL。
 * @property fileUrl 购买后可从此远程 URL 下载文件。
 * @property fileType 数字文件格式（PDF、DOCX 等）。
 * @property pageCount 页数（对文档有意义；其他类型为 0）。
 * @property fileSize 文件大小（字节），用于下载进度估算。
 * @property categoryId 关联到商品所属[Category]的外键。
 * @property salesCount 累计购买次数，用于按热度排序。
 * @property rating 平均用户评分（0.0 - 5.0）。
 * @property isPublished 商品是否对买家可见。隐藏的商品在列表中不显示。
 * @property tags 用于可发现性的可搜索关键词标签。
 * @property createdAt 商品首次上架的毫秒时间戳。
 */
data class Product(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val author: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val coverUrl: String = "",
    val fileUrl: String = "",
    val fileType: FileType = FileType.PDF,
    val pageCount: Int = 0,
    val fileSize: Long = 0L,
    val categoryId: String = "",
    val salesCount: Int = 0,
    val rating: Float = 0f,
    val isPublished: Boolean = true,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 枚举 Sourcehub 市场中支持的数字文件格式。
 *
 * 每个常量携带标准文件扩展名和 MIME 类型，以便下载系统
 * 可以设置正确的 Content-Type 头，文件管理器可以用合适的应用打开文件。
 *
 * @property extension 不带前导点的文件后缀（例如 "pdf"）。
 * @property mimeType 该格式符合 RFC 标准的 MIME 类型。
 */
enum class FileType(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    DOC("doc", "application/msword"),
    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ZIP("zip", "application/zip"),
    OTHER("", "application/octet-stream")
}
