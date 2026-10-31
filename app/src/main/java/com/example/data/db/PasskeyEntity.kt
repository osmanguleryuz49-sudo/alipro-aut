package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passkeys")
data class PasskeyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val relyingParty: String,
    val userName: String,
    val credentialId: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
