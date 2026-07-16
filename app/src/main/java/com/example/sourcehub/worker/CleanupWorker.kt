/**
 * 一个轻量级 [CoroutineWorker]，负责清理由
 * [com.example.sourcehub.data.filestorage.FileStorageManager] 管理的所有临时文件。
 *
 * 此工作器旨在通过 WorkManager 定期（例如每天一次）调度，
 * 使失败或放弃下载产生的过期未加密临时文件不会在磁盘上累积。
 *
 * @property appContext  WorkManager 提供的应用 [Context]
 * @property params       此工作请求的 [WorkerParameters]
 */
package com.example.sourcehub.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.data.filestorage.FileStorageManager

class CleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    /**
     * 删除应用临时存储目录中的所有文件。
     *
     * 这是一个尽力而为的操作——单个文件删除失败会被
     * [FileStorageManager.clearTempFiles] 静默忽略，且工作器始终报告
     * [Result.success]，以免定期调度被中断。
     *
     * @return 始终返回 [Result.success]
     */
    override suspend fun doWork(): Result {
        val storageManager = FileStorageManager(applicationContext)
        storageManager.clearTempFiles()
        return Result.success()
    }
}
