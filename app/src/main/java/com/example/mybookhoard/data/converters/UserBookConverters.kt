package com.example.mybookhoard.data.converters

import androidx.room.TypeConverter
import com.example.mybookhoard.data.entities.UserBookReadingStatus
import com.example.mybookhoard.data.entities.UserBookWishlistStatus

class UserBookConverters {

    // UserBook ReadingStatus converters
    @TypeConverter
    fun fromUserBookReadingStatus(status: UserBookReadingStatus): String = status.name

    @TypeConverter
    fun toUserBookReadingStatus(status: String): UserBookReadingStatus =
        UserBookReadingStatus.valueOf(status)

    // UserBook WishlistStatus converters
    @TypeConverter
    fun fromUserBookWishlistStatus(status: UserBookWishlistStatus?): String? = status?.name

    @TypeConverter
    fun toUserBookWishlistStatus(status: String?): UserBookWishlistStatus? =
        status?.let { UserBookWishlistStatus.valueOf(it) }
}