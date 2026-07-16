/**
 * **SourceHub 市场的演示 / 种子数据**
 *
 * 本文件用一组逼真的数字产品填充 [Products] 表，
 * 以便应用在首次启动时有内容可显示。数据仅在 products 表为空时插入
 * （在 [com.sourcehub.server.plugins.DatabaseFactory.init] 中检查）。
 *
 * ## 种子数据内容
 * 12 个产品，涵盖 5 个分类：
 * - 简历模板 (3)
 * - PPT 模板 (3)
 * - 电子书 (2)
 * - 学习笔记 (2)
 * - 商业文档 (2)
 *
 * ## 模拟/仿真说明
 * - **图片**：`coverUrl` 指向 `picsum.photos` 占位图片。
 *   在生产环境中替换为真实的 CDN 托管封面图片。
 * - **文件**：每个产品的 `filePath` 均为 `"sample.pdf"` 或 `"sample.pptx"`。
 *   这些不是真实存在的文件；下载端点
 *   （[com.sourcehub.server.routes.FileRoutes]）会回退到动态生成一个最小有效的 PDF。
 *   在生产环境中：
 *   - 将实际产品文件上传到对象存储（S3、MinIO）。
 *   - 将对象键存储在 `filePath` 中。
 *   - 通过预签名 URL 提供下载服务。
 * - **销量和评分**：硬编码用于演示效果。在生产环境中，这些应
 *   从实际 [Orders] 和独立的评分表中派生。
 * - **ID**：为可读性使用如 `"p1"` 的短 ID。在生产环境中使用
 *   基于 UUID 或 nanoid 的标识符，以避免跨环境冲突。
 *
 * ## 国际化准备
 * 产品标题、描述和分类使用中文，因为初始目标市场是中文用户。
 * 如果应用扩展到其他语言，应添加 `i18n` 层
 * （例如带有 `locale`、`title`、`description` 的 `product_i18n` 表）。
 */
package com.sourcehub.server.models

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * 使用 [batchInsert] 将演示产品插入 [Products] 表以提高效率。
 * 此函数应仅在 products 表为空时调用，以避免重启时产生重复数据。
 */
object SeedData {
    /** 在单个批次中插入所有演示产品到数据库。 */
    fun insertAll() {
        Products.batchInsert(products) { p ->
            this[Products.id] = p.id
            this[Products.title] = p.title
            this[Products.description] = p.description
            this[Products.author] = p.author
            this[Products.price] = p.price
            this[Products.originalPrice] = p.originalPrice
            this[Products.coverUrl] = p.coverUrl
            this[Products.filePath] = p.filePath
            this[Products.fileType] = p.fileType
            this[Products.pageCount] = p.pageCount
            this[Products.fileSize] = p.fileSize
            this[Products.category] = p.category
            this[Products.salesCount] = p.salesCount
            this[Products.rating] = p.rating
            this[Products.tags] = p.tags
            this[Products.createdAt] = System.currentTimeMillis()
        }
    }

    /**
     * 单条种子产品行的内部数据持有者。
     * 镜像 [Products] 表的列。
     */
    data class SeedProduct(
        val id: String, val title: String, val description: String,
        val author: String, val price: Double, val originalPrice: Double,
        val coverUrl: String, val filePath: String, val fileType: String,
        val pageCount: Int, val fileSize: Long, val category: String,
        val salesCount: Int, val rating: Float, val tags: String
    )

    // 所有价格以人民币 (CNY) 为单位。originalPrice 是在 UI 上显示的
    // "划线价"，用于传达折扣价值。
    private val products = listOf(
        SeedProduct("p1", "高级简历模板 - 创意设计风格", "一套精美的简历模板，适用于设计师和创意行业求职者。包含3种配色方案。", "设计工作室A", 9.90, 19.90, "https://picsum.photos/400/560?random=10", "sample.pdf", "PDF", 2, 1048576, "简历模板", 1280, 4.8f, "简历,创意,设计"),
        SeedProduct("p2", "商务简约简历模板", "经典商务风格简历模板，简洁大方，适合金融、法律、咨询等行业。", "简历大师", 6.90, 12.90, "https://picsum.photos/400/560?random=11", "sample.pdf", "DOCX", 2, 2097152, "简历模板", 950, 4.6f, "简历,商务,简约"),
        SeedProduct("p3", "应届生简历模板套装", "专为应届毕业生设计的简历模板套装，含中英文版本，附带求职信模板。", "CareerLab", 15.90, 29.90, "https://picsum.photos/400/560?random=12", "sample.pdf", "PDF", 5, 5242880, "简历模板", 2340, 4.9f, "简历,应届生,求职"),
        SeedProduct("p4", "科技公司PPT路演模板", "专为科技创业公司设计的融资路演PPT模板，完整页面结构。", "PitchPro", 29.90, 49.90, "https://picsum.photos/400/560?random=13", "sample.pptx", "PPTX", 30, 15728640, "PPT模板", 890, 4.7f, "PPT,路演,创业"),
        SeedProduct("p5", "年终总结汇报PPT", "万能年终总结PPT模板，涵盖工作回顾、业绩展示、明年计划等模块。", "SlideMaster", 19.90, 39.90, "https://picsum.photos/400/560?random=14", "sample.pptx", "PPTX", 25, 12582912, "PPT模板", 1560, 4.5f, "PPT,年终总结,汇报"),
        SeedProduct("p6", "教育机构宣传PPT模板", "适合教育培训机构使用的宣传演示模板，清新活泼的设计风格。", "EduDesign", 12.90, 24.90, "https://picsum.photos/400/560?random=15", "sample.pptx", "PPTX", 18, 8388608, "PPT模板", 670, 4.3f, "PPT,教育,宣传"),
        SeedProduct("p7", "Python编程从入门到实践", "Amazon五星畅销编程书籍，涵盖Python基础、数据分析、Web开发。", "编程大师", 39.90, 79.90, "https://picsum.photos/400/560?random=16", "sample.pdf", "PDF", 480, 25165824, "电子书", 3200, 4.9f, "Python,编程,入门"),
        SeedProduct("p8", "产品经理面试指南", "涵盖产品经理面试常见问题、案例分析框架和答题技巧。", "PM学院", 25.90, 45.90, "https://picsum.photos/400/560?random=17", "sample.pdf", "PDF", 200, 10485760, "电子书", 1890, 4.7f, "产品经理,面试,求职"),
        SeedProduct("p9", "算法导论精讲笔记", "MIT算法课程的完整中文笔记，包含详细图解和练习题解答。", "AlgoNotes", 18.90, 35.90, "https://picsum.photos/400/560?random=18", "sample.pdf", "PDF", 350, 18874368, "学习笔记", 1450, 4.8f, "算法,笔记,面试"),
        SeedProduct("p10", "CPA会计备考笔记", "CPA注册会计师会计科目完整备考笔记，重点考点标注。", "考证达人", 22.90, 42.90, "https://picsum.photos/400/560?random=19", "sample.pdf", "PDF", 280, 14680064, "学习笔记", 980, 4.6f, "CPA,会计,考证"),
        SeedProduct("p11", "创业商业计划书模板", "完整的商业计划书模板，包含公司概述、产品服务、市场分析等章节。", "BizTemplates", 35.90, 69.90, "https://picsum.photos/400/560?random=20", "sample.docx", "DOCX", 45, 20971520, "商业文档", 760, 4.5f, "商业计划,创业,模板"),
        SeedProduct("p12", "市场营销策划方案模板", "专业市场营销策划方案模板，适用于各类产品和服务的营销活动策划。", "MarketingPro", 16.90, 32.90, "https://picsum.photos/400/560?random=21", "sample.docx", "DOCX", 22, 11534336, "商业文档", 540, 4.4f, "营销,策划,模板")
    )
}
