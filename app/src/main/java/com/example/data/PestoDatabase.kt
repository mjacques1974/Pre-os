package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PestoEntity::class], version = 2, exportSchema = false)
abstract class PestoDatabase : RoomDatabase() {
    abstract fun pestoDao(): PestoDao

    companion object {
        @Volatile
        private var INSTANCE: PestoDatabase? = null

        fun getDatabase(context: Context): PestoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PestoDatabase::class.java,
                    "pesto_database.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
