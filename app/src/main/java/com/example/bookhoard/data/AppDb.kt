package com.example.bookhoard.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Database
import androidx.room.TypeConverters

@Database(entities = [Book::class], version = 4)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile private var I: AppDb? = null
        fun get(ctx: Context): AppDb =
            I ?: synchronized(this) {
                I ?: Room.databaseBuilder(ctx, AppDb::class.java, DB_NAME)
                    .fallbackToDestructiveMigration() // 👈 borra y recrea si cambia el esquema
                    .build().also { I = it }
            }

        fun closeDb() { I?.close(); I = null }

        const val DB_NAME = "bookhoard.db"
    }
}