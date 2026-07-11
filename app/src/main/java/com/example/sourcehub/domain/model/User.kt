package com.example.sourcehub.domain.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
