package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PestoDao {
    @Query("SELECT * FROM pesto_products ORDER BY priceBrl ASC")
    fun getAllProductsFlow(): Flow<List<PestoEntity>>

    @Query("SELECT * FROM pesto_products WHERE category = :category ORDER BY priceBrl ASC")
    fun getProductsByCategoryFlow(category: String): Flow<List<PestoEntity>>

    @Query("SELECT * FROM pesto_products WHERE category = :category")
    suspend fun getProductsByCategory(category: String): List<PestoEntity>

    @Query("SELECT * FROM pesto_products")
    suspend fun getAllProducts(): List<PestoEntity>

    @Query("SELECT * FROM pesto_products WHERE id = :id")
    suspend fun getProductById(id: Long): PestoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<PestoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: PestoEntity): Long

    @Update
    suspend fun update(product: PestoEntity)

    @Delete
    suspend fun delete(product: PestoEntity)

    @Query("DELETE FROM pesto_products")
    suspend fun deleteAll()

    @Query("DELETE FROM pesto_products WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Query("SELECT COUNT(*) FROM pesto_products")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM pesto_products WHERE category = :category")
    suspend fun countByCategory(category: String): Int
}
