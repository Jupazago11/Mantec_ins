package com.example.mantec_ins.data.repository

import android.util.Log
import com.example.mantec_ins.data.local.WeeklyElementStatusCacheDao
import com.example.mantec_ins.data.local.WeeklyElementStatusCacheEntity
import com.example.mantec_ins.data.remote.AuthApiService
import java.util.Calendar

class WeeklyElementStatusRepository(
    private val apiService: AuthApiService,
    private val cacheDao: WeeklyElementStatusCacheDao
) {

    suspend fun refreshFromServer(
        areaId: Long,
        elementTypeId: Long
    ): Boolean {
        return try {
            val calendar = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                minimalDaysInFirstWeek = 4
            }

            val week = calendar.get(Calendar.WEEK_OF_YEAR)
            val year = calendar.weekYear

            val remoteItems = apiService.getWeeklyElementsStatus(
                areaId = areaId,
                elementTypeId = elementTypeId
            )

            Log.d(
                "WEEKLY_ELEMENT_STATUS",
                "refreshFromServer areaId=$areaId elementTypeId=$elementTypeId -> remoteItems=${remoteItems.size}"
            )

            val cacheItems = remoteItems.map {
                WeeklyElementStatusCacheEntity(
                    areaId = areaId,
                    elementTypeId = elementTypeId,
                    elementId = it.element_id,
                    elementName = it.element_name,
                    status = it.status,
                    expectedCount = it.expected_count,
                    doneCount = it.done_count,
                    week = week,
                    year = year
                )
            }

            cacheDao.deleteByAreaAndElementType(areaId, elementTypeId)
            cacheDao.insertAll(cacheItems)

            true
        } catch (e: Exception) {
            Log.e(
                "WEEKLY_ELEMENT_STATUS",
                "Error refrescando estado semanal de activos para areaId=$areaId elementTypeId=$elementTypeId",
                e
            )
            false
        }
    }

    suspend fun getCachedStatus(
        areaId: Long,
        elementTypeId: Long
    ): List<WeeklyElementStatusCacheEntity> {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
        }

        val week = calendar.get(Calendar.WEEK_OF_YEAR)
        val year = calendar.weekYear

        return cacheDao.getByAreaAndElementTypeAndWeek(
            areaId = areaId,
            elementTypeId = elementTypeId,
            week = week,
            year = year
        )
    }
}