package com.example.sourcehub.domain.model

/**
 * 表示市场中商品分类的领域模型。
 *
 * 分类用于分组商品，在浏览界面中以标签或选项卡形式显示。
 * 每个分类维护一个商品数量，可用于展示角标或隐藏空分类。
 *
 * @property id 唯一分类标识符（例如 "cat_1"）。
 * @property name 在界面中显示的展示名称（例如 "简历模板"）。
 * @property iconName 分类图标的 Material 图标名称或 drawable 资源别名。
 * @property sortOrder 决定显示顺序（值越小越靠前）。
 * @property productCount 此分类中已发布商品的数量。
 */
data class Category(
    val id: String = "",
    val name: String = "",
    val iconName: String = "",
    val sortOrder: Int = 0,
    val productCount: Int = 0
)
