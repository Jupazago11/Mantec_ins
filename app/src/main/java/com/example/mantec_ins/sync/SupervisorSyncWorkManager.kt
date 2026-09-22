package com.example.mantec_ins.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// Calco de SyncWorkManager (Inspector), aparte para no tocar el
// existente — mismo intervalo/constraint, sin el chequeo de
// group.autoSync (no aplica a Employee).
object SupervisorSyncWorkManager {

    private const val WORK_NAME = "supervisor_sync_worker"

    fun start(context: Context) {
        try {
            val appContext = context.applicationContext

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SupervisorSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } catch (e: Exception) {
            Log.e("SUPERVISOR_SYNC_WORK_MANAGER", "Error configurando WorkManager periódico", e)
        }
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }
}
