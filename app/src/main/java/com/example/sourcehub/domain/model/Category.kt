package com.example.sourcehub.domain.model

data class Category(
    val id: String = "",
    val name: String = "",
    val iconName: String = "",
    val sortOrder: Int = 0,
    val productCount: Int = 0
)
