package com.example.mantec_ins.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mantec_ins.data.local.DatabaseProvider
import com.example.mantec_ins.data.remote.RetrofitClient
import com.example.mantec_ins.data.repository.SupervisorSyncRepository

// Calco de SyncWorker (Inspector) — sin el gate de group.autoSync (ese
// flag es de GroupEntity, un concepto de cliente/agrupacion que no
// existe para Employee). El sync del supervisor corre siempre que haya
// red; el usuario tiene el mismo botón manual de escape.
class SupervisorSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("SUPERVISOR_SYNC_WORKER", "Iniciando sync en background")

            val db = DatabaseProvider.getDatabase(applicationContext)
            val api = RetrofitClient.createPersonalApiService(applicationContext, background = true)
            val repository = SupervisorSyncRepository(db = db, api = api)

            repository.sync()

            Log.d("SUPERVISOR_SYNC_WORKER", "Sync finalizado")
            Result.success()
        } catch (e: Exception) {
            Log.e("SUPERVISOR_SYNC_WORKER", "Error en sync", e)
            Result.retry()
        }
    }
}
