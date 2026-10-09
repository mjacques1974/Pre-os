package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pesto_products")
data class PestoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String = ProductCategory.PESTO.id,
    val brand: String,
    val name: String,
    val weightGrams: Int,
    val supermarket: String,
    val priceBrl: Double,
    val previousPriceBrl: Double?,
    val lastUpdated: Long,
    val origin: String,
    val ingredientsNote: String,
    val isPromo: Boolean
) {
    fun toProduct(): PestoProduct {
        return PestoProduct(
            id = id,
            category = category,
            brand = brand,
            name = name,
            weightGrams = weightGrams,
            supermarket = supermarket,
            priceBrl = priceBrl,
            previousPriceBrl = previousPriceBrl,
            lastUpdated = lastUpdated,
            origin = origin,
            ingredientsNote = ingredientsNote,
            isPromo = isPromo
        )
    }

    companion object {
        fun fromProduct(product: PestoProduct): PestoEntity {
            return PestoEntity(
                id = product.id,
                category = product.category,
                brand = product.brand,
                name = product.name,
                weightGrams = product.weightGrams,
                supermarket = product.supermarket,
                priceBrl = product.priceBrl,
                previousPriceBrl = product.previousPriceBrl,
                lastUpdated = product.lastUpdated,
                origin = product.origin,
                ingredientsNote = product.ingredientsNote,
                isPromo = product.isPromo
            )
        }
    }
}
