package com.example.mantec_ins.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ClientEntity::class,
        GroupEntity::class,
        AreaEntity::class,
        ElementTypeEntity::class,
        ElementEntity::class,
        ComponentEntity::class,
        DiagnosticEntity::class,
        ConditionEntity::class,
        ReportEntity::class,
        ReportDetailEntity::class,
        EvidenceEntity::class,
        ElementComponentCrossRef::class,
        ComponentDiagnosticCrossRef::class,
        WeeklyElementStatusCacheEntity::class,
        WeeklyDiagnosticStatusCacheEntity::class,
        ComponentConditionCrossRef::class,
        MeasurementThicknessDraftEntity::class,
        MeasurementThicknessDraftLineEntity::class,
        MeasurementElementTypeAccessEntity::class,
        SupervisorActivityEntity::class,
        SupervisorPersonaEntity::class,
        SupervisorEvidenceEntity::class
    ],
    version = 21,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun groupDao(): GroupDao
    abstract fun areaDao(): AreaDao
    abstract fun elementTypeDao(): ElementTypeDao
    abstract fun elementDao(): ElementDao
    abstract fun componentDao(): ComponentDao
    abstract fun diagnosticDao(): DiagnosticDao
    abstract fun conditionDao(): ConditionDao
    abstract fun elementComponentDao(): ElementComponentDao
    abstract fun componentDiagnosticDao(): ComponentDiagnosticDao
    abstract fun catalogRelationDao(): CatalogRelationDao
    abstract fun reportDao(): ReportDao
    abstract fun pendingDiagnosticCacheDao(): PendingDiagnosticCacheDao
    abstract fun weeklyElementStatusCacheDao(): WeeklyElementStatusCacheDao
    abstract fun reportDetailDao(): ReportDetailDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun componentConditionDao(): ComponentConditionDao
    abstract fun measurementThicknessDao(): MeasurementThicknessDao
    abstract fun measurementElementTypeAccessDao(): MeasurementElementTypeAccessDao
    abstract fun supervisorActivityDao(): SupervisorActivityDao
    abstract fun supervisorPersonaDao(): SupervisorPersonaDao
    abstract fun supervisorEvidenceDao(): SupervisorEvidenceDao
}