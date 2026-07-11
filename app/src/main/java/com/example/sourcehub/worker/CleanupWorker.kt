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

    override suspend fun doWork(): Result {
        val storageManager = FileStorageManager(applicationContext)
        storageManager.clearTempFiles()
        return Result.success()
    }
}
