package com.example.mantec_ins.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Migracion NO destructiva 19 -> 20: agrega las 3 tablas del modulo
// Supervisor offline, sin tocar ninguna tabla existente. Critico para no
// perder reportes PENDING_SYNC reales de Inspectores en produccion al
// actualizar la app (ver PATRONES_ASINCRONISMO_OFFLINE.md patron 5 —
// fallbackToDestructiveMigration() solo corre en debug, en release esta
// migracion es la unica forma de subir de version sin borrar todo).
//
// DDL calcado EXACTO del que genera Room/KSP para estas 3 entidades
// (ver app/schemas/.../20.json, generado con room.schemaLocation) — no
// escrito a mano por adivinanza, para que la validacion de esquema de
// Room al abrir la base coincida con lo que espera.
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `supervisor_activities` (" +
                "`id` INTEGER NOT NULL, `date` TEXT NOT NULL, `companyName` TEXT NOT NULL, " +
                "`team` TEXT, `process` TEXT, `description` TEXT NOT NULL, " +
                "`activityType` TEXT NOT NULL, `shift` TEXT NOT NULL, `estimatedHours` REAL, " +
                "`closed` INTEGER NOT NULL, `comments` TEXT, `allWorkedScheduledHours` INTEGER, " +
                "`reportedHours` REAL, `registrado` INTEGER NOT NULL, " +
                "`registrationSyncStatus` TEXT NOT NULL, `lastError` TEXT, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `supervisor_personas` (" +
                "`activityId` INTEGER NOT NULL, `employeeId` INTEGER NOT NULL, " +
                "`nombre` TEXT NOT NULL, `nickname` TEXT NOT NULL, `workedHours` REAL, " +
                "PRIMARY KEY(`activityId`, `employeeId`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `supervisor_evidences` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `activityId` INTEGER NOT NULL, " +
                "`localPath` TEXT NOT NULL, `originalName` TEXT NOT NULL, `fileType` TEXT NOT NULL, " +
                "`syncStatus` TEXT NOT NULL, `serverId` INTEGER, `lastError` TEXT)"
        )
    }
}

// Migracion NO destructiva 20 -> 21: agrega la columna `comentario` a
// supervisor_personas (pedido 2026-09-22, comentario del responsable
// sobre las horas reportadas de un trabajador puntual) — ALTER TABLE ADD
// COLUMN nullable, no rompe filas existentes (llegan con NULL). Sin
// clausula DEFAULT explicita a proposito: app/schemas/.../21.json (el
// esquema real que genera Room/KSP para SupervisorPersonaEntity) no
// declara defaultValue para esta columna — Room valida TableInfo
// (incluyendo defaultValue) al abrir la base, y agregar DEFAULT NULL aqui
// desalinearia esa comparacion con lo que Room espera.
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `supervisor_personas` ADD COLUMN `comentario` TEXT")
    }
}
