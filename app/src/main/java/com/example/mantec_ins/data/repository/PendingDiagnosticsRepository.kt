package com.example.mantec_ins.data.repository

import android.util.Log
import com.example.mantec_ins.data.local.PendingDiagnosticCacheDao
import com.example.mantec_ins.data.local.WeeklyDiagnosticStatusCacheEntity
import com.example.mantec_ins.data.remote.AuthApiService
import java.util.Calendar

class PendingDiagnosticsRepository(
    private val apiService: AuthApiService,
    private val cacheDao: PendingDiagnosticCacheDao
) {

    suspend fun refreshFromServer(elementId: Long): Boolean {
        return try {
            val calendar = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                minimalDaysInFirstWeek = 4
            }

            val week = calendar.get(Calendar.WEEK_OF_YEAR)
            val year = calendar.weekYear

            val remoteItems = apiService.getWeeklyDiagnosticStatus(elementId)

            remoteItems.forEach {
                Log.d(
                    "WEEKLY_STATUS_ITEM",
                    "elementId=$elementId component=${it.component_name} diagnostic=${it.diagnostic_name} status=${it.status}"
                )
            }

            Log.d(
                "WEEKLY_STATUS",
                "refreshFromServer elementId=$elementId -> remoteItems=${remoteItems.size}"
            )

            val cacheItems = remoteItems.map {
                WeeklyDiagnosticStatusCacheEntity(
                    elementId = elementId,
                    componentId = it.component_id,
                    diagnosticId = it.diagnostic_id,
                    diagnosticName = it.diagnostic_name,
                    componentName = it.component_name,
                    status = it.status.trim().uppercase(),
                    week = week,
                    year = year
                )
            }

            cacheDao.deleteByElement(elementId)
            cacheDao.insertAll(cacheItems)

            true
        } catch (e: Exception) {
            Log.e(
                "WEEKLY_STATUS",
                "Error refrescando estado semanal desde servidor para elementId=$elementId",
                e
            )
            false
        }
    }

    suspend fun getCachedStatusForElement(elementId: Long): List<WeeklyDiagnosticStatusCacheEntity> {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
        }

        val week = calendar.get(Calendar.WEEK_OF_YEAR)
        val year = calendar.weekYear

        return cacheDao.getByElementAndWeek(
            elementId = elementId,
            week = week,
            year = year
        )
    }
}