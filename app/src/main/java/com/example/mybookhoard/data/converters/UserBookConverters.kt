package com.example.mybookhoard.data.converters

import androidx.room.TypeConverter
import com.example.mybookhoard.data.entities.UserBookReadingStatus
import com.example.mybookhoard.data.entities.UserBookWishlistStatus

class UserBookConverters {

    // UserBook WishlistStatus converters
    @TypeConverter
    fun fromUserBookWishlistStatus(status: UserBookWishlistStatus?): String? = status?.name

    @TypeConverter
    fun toUserBookWishlistStatus(status: String?): UserBookWishlistStatus? =
        status?.let { UserBookWishlistStatus.valueOf(it) }
}