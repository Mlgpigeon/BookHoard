package com.example.mybookhoard.data.converters

import androidx.room.TypeConverter
import com.example.mybookhoard.data.entities.BookSource
import com.example.mybookhoard.data.entities.UserBookReadingStatus
import com.example.mybookhoard.data.entities.UserBookWishlistStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    // Date converters
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    // BookSource enum converter
    @TypeConverter
    fun fromBookSource(source: BookSource): String = source.name

    @TypeConverter
    fun toBookSource(source: String): BookSource = enumValueOf(source)

    // UserBook enum converters
    @TypeConverter
    fun fromUserBookReadingStatus(status: UserBookReadingStatus?): String? = status?.name

    @TypeConverter
    fun toUserBookReadingStatus(status: String?): UserBookReadingStatus? =
        status?.let { enumValueOf<UserBookReadingStatus>(it) }

    @TypeConverter
    fun fromUserBookWishlistStatus(status: UserBookWishlistStatus?): String? = status?.name

    @TypeConverter
    fun toUserBookWishlistStatus(status: String?): UserBookWishlistStatus? =
        status?.let { enumValueOf<UserBookWishlistStatus>(it) }


    // List<String> converters for JSON arrays (genres, images, adaptations)
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            if (it.isBlank()) return null
            try {
                val listType = object : TypeToken<List<String>>() {}.type
                gson.fromJson(it, listType)
            } catch (e: Exception) {
                null
            }
        }
    }
}