package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY isPinned DESC, orderIndex ASC, id DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("UPDATE accounts SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE accounts SET orderIndex = :newIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, newIndex: Int)

    @Query("UPDATE accounts SET counter = counter + 1 WHERE id = :id")
    suspend fun incrementCounter(id: Long)

    // Passkey queries
    @Query("SELECT * FROM passkeys ORDER BY id DESC")
    fun getAllPasskeys(): Flow<List<PasskeyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPasskey(passkey: PasskeyEntity): Long

    @Delete
    suspend fun deletePasskey(passkey: PasskeyEntity)
}
