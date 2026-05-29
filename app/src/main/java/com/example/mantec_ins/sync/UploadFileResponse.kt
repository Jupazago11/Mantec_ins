package com.example.mantec_ins.sync

data class UploadFileResponse(
    val success: Boolean,
    val message: String,
    val file_id: Long,
    val path: String,
    val file_type: String
)
