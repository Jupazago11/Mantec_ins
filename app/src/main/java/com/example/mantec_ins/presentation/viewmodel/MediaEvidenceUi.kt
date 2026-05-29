package com.example.mantec_ins.presentation.viewmodel

data class MediaEvidenceUi(
    val path: String,
    val type: String // "image" o "video"
) {
    val fileName: String
        get() = path.substringAfterLast("/")
}