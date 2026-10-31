package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serviceName: String,
    val accountName: String,
    val encryptedSecret: String, // Android KeyStore AES-GCM encrypted Base64 string
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30,
    val type: String = "TOTP", // "TOTP", "HOTP", "STEAM"
    val counter: Long = 0L,     // For HOTP counter
    val category: String = "Genel", // "Genel", "İş", "Kişisel", "Kripto", "Oyun"
    val isPinned: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
