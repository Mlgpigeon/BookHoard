package com.example.mybookhoard.data

import org.json.JSONObject

data class User(
    val id: Long,
    val username: String,
    val email: String,
    val role: String,
    val createdAt: String,
    val isActive: Boolean
) {
    companion object {
        fun fromJson(json: JSONObject): User {
            return User(
                id = json.getLong("id"),
                username = json.getString("username"),
                email = json.getString("email"),
                role = json.getString("role"),
                createdAt = json.getString("created_at"),
                isActive = json.getBoolean("is_active")
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("username", username)
            put("email", email)
            put("role", role)
            put("created_at", createdAt)
            put("is_active", isActive)
        }
    }
}