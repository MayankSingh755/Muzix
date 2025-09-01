package com.ionic.muzix.data.database

import android.app.Application
import androidx.room.Room

class MyApplication : Application() {
    lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "muzix_database"
        ).build()
    }
}