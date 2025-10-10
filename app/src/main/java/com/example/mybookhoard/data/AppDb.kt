package com.example.mybookhoard.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Database
import androidx.room.TypeConverters
import com.example.mybookhoard.data.dao.AuthorDao
import com.example.mybookhoard.data.dao.BookDao
import com.example.mybookhoard.data.dao.UserBookDao
import com.example.mybookhoard.data.entities.*
import com.example.mybookhoard.data.converters.*

@Database(
    entities = [
        Author::class,
        Book::class,
        UserBook::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {

    abstract fun authorDao(): AuthorDao
    abstract fun bookDao(): BookDao
    abstract fun userBookDao(): UserBookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDb? = null

        fun get(context: Context): AppDb {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration() // For development
                    .build().also { INSTANCE = it }
            }
        }

        fun closeDb() {
            INSTANCE?.close()
            INSTANCE = null
        }

        const val DB_NAME = "mybookhoard.db"
    }
}