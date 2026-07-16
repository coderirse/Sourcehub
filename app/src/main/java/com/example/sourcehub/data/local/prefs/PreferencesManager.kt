package com.example.sourcehub.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 顶层扩展属性，为每个 [Context] 创建一个 [DataStore] 实例。
 * 命名为 `"sourcehub_settings"`，由应用内部存储中的 protobuf 编码
 * 偏好设置文件支持。
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sourcehub_settings")

/**
 * 基于 Jetpack DataStore 的应用级偏好设置轻量包装器。
 *
 * ## 存储的偏好设置
 * - **最近搜索**：最近 10 条搜索查询的管道分隔（`|||`）列表，
 *   按 MRU（最近使用）顺序维护。以挂起函数和响应式 [Flow] 两种方式暴露。
 * - **仅 Wi-Fi 下载**：布尔标志（默认 `true`）。启用时，
 *   [DownloadWorker] 会受限于非计量网络连接。
 * - **生物识别锁**：布尔标志（默认 `false`）。启用时，
 *   应用在启动时要求生物识别认证。
 * - **使用远程 API**：布尔标志（默认 `false`）。在
 *   模拟 API（本地）和 Retrofit API（Ktor 后端）之间切换。
 *
 * ## 线程安全
 * 所有写入都通过 [DataStore.edit] 进行，它是原子操作且
 * 协程安全的。通过 [Flow] 的读取始终保持一致。
 */
class PreferencesManager(private val context: Context) {

    // ── 最近搜索 ────────────────────────────────────────────

    /**
     * 添加一条查询到最近搜索列表。
     * 如果该查询已存在，则将其移到最前面（MRU 顺序）。
     * 列表最多保留 [MAX_RECENT_SEARCHES] 条记录。
     */
    suspend fun addRecentSearch(query: String) {
        val searches = getRecentSearches().toMutableList()
        searches.remove(query)       // 去重：移除已存在的记录。
        searches.add(0, query)       // 插入到最前面以保持 MRU 顺序。
        if (searches.size > MAX_RECENT_SEARCHES) {
            searches.removeAt(searches.lastIndex) // 淘汰最旧的记录。
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_RECENT_SEARCHES] = searches.joinToString(SEPARATOR)
        }
    }

    /** 返回当前最近搜索列表的快照（最新在前）。 */
    suspend fun getRecentSearches(): List<String> {
        var result = emptyList<String>()
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_RECENT_SEARCHES] ?: ""
            result = raw.split(SEPARATOR).filter { it.isNotEmpty() }
        }
        return result
    }

    /** 清除所有最近搜索。 */
    suspend fun clearRecentSearches() {
        context.dataStore.edit { it.remove(KEY_RECENT_SEARCHES) }
    }

    /** 响应式观察最近搜索列表。 */
    val recentSearchesFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_RECENT_SEARCHES] ?: ""
        raw.split(SEPARATOR).filter { it.isNotEmpty() }
    }

    // ── 应用设置 ────────────────────────────────────────────────

    /** 观察"仅 Wi-Fi 下载"设置（默认：true）。 */
    val wifiOnlyDownload: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIFI_ONLY] ?: true
    }

    /** 更新"仅 Wi-Fi 下载"偏好设置。 */
    suspend fun setWifiOnlyDownload(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WIFI_ONLY] = enabled }
    }

    /** 观察生物识别锁设置（默认：false）。 */
    val biometricLock: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_LOCK] ?: false
    }

    /** 更新生物识别锁偏好设置。 */
    suspend fun setBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRIC_LOCK] = enabled }
    }

    /**
     * 观察远程 API 开关（默认：false）。
     * 为 false 时使用模拟 API 实现，为 true 时使用
     * 指向 Ktor 后端的 Retrofit 实现。
     */
    val useRemoteApi: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_REMOTE_API] ?: false
    }

    /** 在模拟和远程 API 后端之间切换。 */
    suspend fun setUseRemoteApi(enabled: Boolean) {
        context.dataStore.edit { it[KEY_REMOTE_API] = enabled }
    }

    companion object {
        // 偏好设置键 — 类型化以实现类型安全的读写。
        private val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        private val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only_download")
        private val KEY_BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        private val KEY_REMOTE_API = booleanPreferencesKey("use_remote_api")

        /** 持久化的最近搜索最大数量。 */
        private const val MAX_RECENT_SEARCHES = 10

        /**
         * 用于序列化最近搜索列表的分隔符。
         * 选择 `|||` 是因为它不太可能出现在搜索查询中。
         */
        private const val SEPARATOR = "|||"
    }
}
