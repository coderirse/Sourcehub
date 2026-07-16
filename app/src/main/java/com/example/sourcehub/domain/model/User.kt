package com.example.sourcehub.domain.model

/**
 * 表示已认证用户的领域模型。
 *
 * 此数据类保存 Sourcehub 平台注册用户的核心身份和资料信息。
 * 它贯穿用于认证流程、资料管理，并作为订单、购物车项和下载的所有者键。
 *
 * @property id 唯一用户标识符，通常由后端分配（例如 "user_001"）。
 * @property name 在界面和用户资料中显示的展示名称。
 * @property email 注册邮箱地址，同时用作登录凭据。
 * @property avatarUrl 用户头像图片的远程 URL。
 * @property phone 用于账户恢复和通知的脱敏手机号。
 * @property createdAt 账户创建的毫秒时间戳。
 */
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
