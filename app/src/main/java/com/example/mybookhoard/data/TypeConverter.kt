package com.example.mybookhoard.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(value: ReadingStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): ReadingStatus =
        enumValueOf(value)

    @TypeConverter
    fun fromWishlistStatus(value: WishlistStatus?): String? = value?.name

    @TypeConverter
    fun toWishlistStatus(value: String?): WishlistStatus? =
        value?.let { enumValueOf<WishlistStatus>(it) }
}
