package com.example.mybookhoard.data

import androidx.room.TypeConverter
import java.util.Date

class UserBookConverters {

    // Date converters
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // UserBookReadingStatus converters
    @TypeConverter
    fun fromUserBookReadingStatus(status: UserBookReadingStatus): String {
        return status.name
    }

    @TypeConverter
    fun toUserBookReadingStatus(status: String): UserBookReadingStatus {
        return UserBookReadingStatus.valueOf(status)
    }

    // UserBookWishlistStatus converters
    @TypeConverter
    fun fromUserBookWishlistStatus(status: UserBookWishlistStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toUserBookWishlistStatus(status: String?): UserBookWishlistStatus? {
        return status?.let { UserBookWishlistStatus.valueOf(it) }
    }
}