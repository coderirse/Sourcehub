package com.example.sourcehub.domain.model

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

enum class FileType(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    DOC("doc", "application/msword"),
    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ZIP("zip", "application/zip"),
    OTHER("", "application/octet-stream")
}
