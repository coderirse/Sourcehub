package com.example.sourcehub.data.local.mock

import com.example.sourcehub.domain.model.*

class MockDataProvider {
    // Shared payment state: orderId -> (transactionId, paidTimestamp)
    val paidOrders = mutableMapOf<String, Pair<String, Long>>()

    val categories = listOf(
        Category("cat_1", "简历模板", "description", 0, 42),
        Category("cat_2", "PPT模板", "slideshow", 1, 38),
        Category("cat_3", "电子书", "book", 2, 56),
        Category("cat_4", "学习笔记", "school", 3, 23),
        Category("cat_5", "商业文档", "business", 4, 31),
        Category("cat_6", "技术文档", "code", 5, 19)
    )

    val banners = listOf(
        Banner("b1", "精品简历模板合集", "https://picsum.photos/800/300?random=1", BannerLinkType.CATEGORY, "cat_1", 0),
        Banner("b2", "Python编程电子书", "https://picsum.photos/800/300?random=2", BannerLinkType.PRODUCT, "prod_10", 1),
        Banner("b3", "年终总结PPT模板", "https://picsum.photos/800/300?random=3", BannerLinkType.CATEGORY, "cat_2", 2)
    )

    val products = listOf(
        Product("prod_1", "高级简历模板 - 创意设计风格", "一套精美的简历模板，适用于设计师和创意行业求职者。包含3种配色方案，支持PDF和DOCX格式导出。", "设计工作室A", 9.90, 19.90, "https://picsum.photos/400/560?random=10", "", FileType.PDF, 2, 1048576, "cat_1", 1280, 4.8f, tags = listOf("简历", "创意", "设计")),
        Product("prod_2", "商务简约简历模板", "经典商务风格简历模板，简洁大方，适合金融、法律、咨询等行业使用。", "简历大师", 6.90, 12.90, "https://picsum.photos/400/560?random=11", "", FileType.DOCX, 2, 2097152, "cat_1", 950, 4.6f, tags = listOf("简历", "商务", "简约")),
        Product("prod_3", "应届生简历模板套装", "专为应届毕业生设计的简历模板套装，含中英文版本，附带求职信模板。", "CareerLab", 15.90, 29.90, "https://picsum.photos/400/560?random=12", "", FileType.PDF, 5, 5242880, "cat_1", 2340, 4.9f, tags = listOf("简历", "应届生", "求职")),
        Product("prod_4", "科技公司PPT路演模板", "专为科技创业公司设计的融资路演PPT模板，包含产品展示、市场分析、财务预测等完整页面结构。", "PitchPro", 29.90, 49.90, "https://picsum.photos/400/560?random=13", "", FileType.PPTX, 30, 15728640, "cat_2", 890, 4.7f, tags = listOf("PPT", "路演", "创业")),
        Product("prod_5", "年终总结汇报PPT", "万能年终总结PPT模板，涵盖工作回顾、业绩展示、明年计划等模块。20+页面布局，高清图表支持。", "SlideMaster", 19.90, 39.90, "https://picsum.photos/400/560?random=14", "", FileType.PPTX, 25, 12582912, "cat_2", 1560, 4.5f, tags = listOf("PPT", "年终总结", "汇报")),
        Product("prod_6", "教育机构宣传PPT模板", "适合教育培训机构使用的宣传演示模板，清新活泼的设计风格。", "EduDesign", 12.90, 24.90, "https://picsum.photos/400/560?random=15", "", FileType.PPTX, 18, 8388608, "cat_2", 670, 4.3f, tags = listOf("PPT", "教育", "宣传")),
        Product("prod_7", "Python编程从入门到实践（第3版）", "Amazon五星畅销编程书籍，涵盖Python基础、数据分析、Web开发和自动化脚本，包含100+实战项目。", "编程大师", 39.90, 79.90, "https://picsum.photos/400/560?random=16", "", FileType.PDF, 480, 25165824, "cat_3", 3200, 4.9f, tags = listOf("Python", "编程", "入门")),
        Product("prod_8", "产品经理面试指南", "涵盖产品经理面试常见问题、案例分析框架和答题技巧。BAT/TMD面试官联合编写。", "PM学院", 25.90, 45.90, "https://picsum.photos/400/560?random=17", "", FileType.PDF, 200, 10485760, "cat_3", 1890, 4.7f, tags = listOf("产品经理", "面试", "求职")),
        Product("prod_9", "算法导论精讲笔记", "MIT算法课程的完整中文笔记，包含详细图解和练习题解答，适合备战技术面试。", "AlgoNotes", 18.90, 35.90, "https://picsum.photos/400/560?random=18", "", FileType.PDF, 350, 18874368, "cat_4", 1450, 4.8f, tags = listOf("算法", "笔记", "面试")),
        Product("prod_10", "CPA会计备考笔记", "CPA注册会计师会计科目完整备考笔记，重点考点标注，真题解析和记忆口诀。", "考证达人", 22.90, 42.90, "https://picsum.photos/400/560?random=19", "", FileType.PDF, 280, 14680064, "cat_4", 980, 4.6f, tags = listOf("CPA", "会计", "考证")),
        Product("prod_11", "创业商业计划书模板", "完整的商业计划书模板，包含公司概述、产品服务、市场分析、营销策略、财务规划等章节。", "BizTemplates", 35.90, 69.90, "https://picsum.photos/400/560?random=20", "", FileType.DOCX, 45, 20971520, "cat_5", 760, 4.5f, tags = listOf("商业计划", "创业", "模板")),
        Product("prod_12", "市场营销策划方案模板", "专业市场营销策划方案模板，适用于各类产品和服务的营销活动策划。", "MarketingPro", 16.90, 32.90, "https://picsum.photos/400/560?random=21", "", FileType.DOCX, 22, 11534336, "cat_5", 540, 4.4f, tags = listOf("营销", "策划", "模板")),
        Product("prod_13", "React Native移动开发实战", "从零学习React Native跨平台移动开发，包含完整项目案例：电商App和社交App。", "CodeHub", 34.90, 59.90, "https://picsum.photos/400/560?random=22", "", FileType.PDF, 420, 22020096, "cat_6", 1230, 4.7f, tags = listOf("React Native", "移动开发", "实战")),
        Product("prod_14", "系统设计面试精讲", "覆盖分布式系统设计的所有核心主题：缓存、消息队列、数据库分片、微服务架构等。", "ArchNotes", 28.90, 49.90, "https://picsum.photos/400/560?random=23", "", FileType.PDF, 380, 19922944, "cat_6", 2100, 4.9f, tags = listOf("系统设计", "面试", "架构")),
        Product("prod_15", "日语能力考N2词汇大全", "完整的N2词汇手册，包含释义、例句、红宝书记忆法，附赠Anki导入词库。", "日语学习社", 11.90, 22.90, "https://picsum.photos/400/560?random=24", "", FileType.PDF, 180, 9437184, "cat_4", 830, 4.5f, tags = listOf("日语", "词汇", "考试")),
        Product("prod_16", "UI/UX设计师作品集模板", "设计师专属在线作品集展示模板，适配多端展示，包含作品详情页和关于我页面。", "DesignLab", 19.90, 39.90, "https://picsum.photos/400/560?random=25", "", FileType.PDF, 15, 7340032, "cat_1", 670, 4.4f, tags = listOf("UI设计", "作品集", "模板")),
        Product("prod_17", "数据分析师SQL进阶教程", "从高级查询到性能优化的完整SQL进阶指南，包含真实业务场景案例分析。", "DataSchool", 27.90, 54.90, "https://picsum.photos/400/560?random=26", "", FileType.PDF, 320, 16777216, "cat_6", 1560, 4.8f, tags = listOf("SQL", "数据分析", "进阶")),
        Product("prod_18", "小红书运营全攻略", "从零开始学习小红书内容创作和账号运营，包含爆款笔记写作方法和涨粉技巧。", "SocialGuru", 14.90, 28.90, "https://picsum.photos/400/560?random=27", "", FileType.PDF, 160, 8388608, "cat_5", 2890, 4.6f, tags = listOf("小红书", "运营", "自媒体")),
        Product("prod_19", "英语四六级写作模板", "四六级英语写作高分模板，包含议论文、图表作文、应用文等各类题型的写作框架和常用句型。", "英语通", 8.90, 16.90, "https://picsum.photos/400/560?random=28", "", FileType.PDF, 80, 4194304, "cat_4", 3450, 4.7f, tags = listOf("英语", "写作", "四六级")),
        Product("prod_20", "企业微信运营SOP手册", "完整的企业微信私域运营标准操作流程手册，包含客户获取、社群运营和转化路径。", "WeComPro", 21.90, 41.90, "https://picsum.photos/400/560?random=29", "", FileType.DOCX, 35, 17825792, "cat_5", 1120, 4.5f, tags = listOf("企业微信", "私域", "运营"))
    )

    val mockUsers = mapOf(
        "test@sourcehub.com" to User(
            id = "user_001",
            name = "测试用户",
            email = "test@sourcehub.com",
            avatarUrl = "https://picsum.photos/200/200?random=99",
            phone = "138****8888",
            createdAt = System.currentTimeMillis() - 86400000 * 30
        )
    )

    fun getProductById(id: String): Product? = products.find { it.id == id }
    fun getProductsByCategory(categoryId: String): List<Product> =
        products.filter { it.categoryId == categoryId }
    fun searchProducts(query: String): List<Product> =
        products.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.author.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
    fun getCategoryById(id: String): Category? = categories.find { it.id == id }
}
