package com.example.mantec_ins.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mantec_ins.data.local.DatabaseProvider
import com.example.mantec_ins.data.remote.RetrofitClient
import com.example.mantec_ins.data.repository.SyncRepository
import com.example.mantec_ins.data.repository.MeasurementThicknessRepository

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("SYNC_WORKER", "Iniciando sync en background")

            val db = DatabaseProvider.getDatabase(applicationContext)

            val group = db.groupDao().getFirst()
            if (group?.autoSync != true) {
                Log.d(
                    "SYNC_WORKER",
                    "SyncWorker cancelado: la agrupación no tiene sincronización automática."
                )
                return Result.success()
            }

            val api = RetrofitClient.createSyncApiService(applicationContext)

            val repository = SyncRepository(
                context = applicationContext,
                db = db,
                api = api
            )

            repository.syncPendingReports()

            val measurementRepository = MeasurementThicknessRepository(
                context = applicationContext,
                database = db,
                api = RetrofitClient.createMeasurementApiService(applicationContext)
            )

            val syncedMeasurementDrafts = measurementRepository.syncAllPendingDrafts()

            Log.d(
                "SYNC_WORKER",
                "Borradores de mediciones sincronizados=$syncedMeasurementDrafts"
            )

            Log.d("SYNC_WORKER", "Sync finalizado correctamente")

            Result.success()

        } catch (e: Exception) {
            Log.e("SYNC_WORKER", "Error en sync", e)
            Result.retry()
        }
    }
}
