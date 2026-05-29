package com.example.mantec_ins.presentation.viewmodel

data class WeeklyElementStatusItemUi(
    val areaId: Long,
    val elementTypeId: Long,
    val elementId: Long,
    val elementName: String,
    val status: String,
    val expectedCount: Int,
    val doneCount: Int
)