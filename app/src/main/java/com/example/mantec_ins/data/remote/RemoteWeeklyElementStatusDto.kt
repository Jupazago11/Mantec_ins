package com.example.mantec_ins.data.remote

data class RemoteWeeklyElementStatusDto(
    val element_id: Long,
    val element_name: String,
    val status: String,
    val expected_count: Int,
    val done_count: Int
)