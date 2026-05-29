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
            )

            if (BuildConfig.DEBUG) {
                builder.fallbackToDestructiveMigration()
            }

            val instance = builder.build()
            INSTANCE = instance
            instance
        }
    }
}