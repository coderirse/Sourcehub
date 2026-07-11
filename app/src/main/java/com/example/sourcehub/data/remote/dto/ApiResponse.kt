package com.example.sourcehub.data.remote.dto

data class ApiResponse<T>(
    val code: Int = 200,
    val message: String = "success",
    val data: T? = null
)
