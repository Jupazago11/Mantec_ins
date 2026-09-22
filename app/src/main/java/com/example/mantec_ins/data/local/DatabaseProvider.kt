package com.example.mantec_ins.data.local

import android.content.Context
import androidx.room.Room
import com.example.mantec_ins.BuildConfig

object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "mantec_inspector_db"
            ).addMigrations(MIGRATION_19_20, MIGRATION_20_21)

            if (BuildConfig.DEBUG) {
                // Red de seguridad solo en debug para cualquier salto de
                // version sin migracion explicita todavia — nunca en
                // release (ver PATRONES_ASINCRONISMO_OFFLINE.md patron 5).
                builder.fallbackToDestructiveMigration()
            }

            val instance = builder.build()
            INSTANCE = instance
            instance
        }
    }
}