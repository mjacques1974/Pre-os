package com.example.data

import java.util.Locale

data class PestoProduct(
    val id: Long = 0,
    val category: String = ProductCategory.PESTO.id,
    val brand: String,
    val name: String,
    val weightGrams: Int,
    val supermarket: String,
    val priceBrl: Double,
    val previousPriceBrl: Double? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val origin: String = "Itália 🇮🇹",
    val ingredientsNote: String = "Manjericão genovês, azeite de oliva e queijo italiano",
    val isPromo: Boolean = false
) {
    val categoryEnum: ProductCategory
        get() = ProductCategory.fromId(category)

    val unitLabel: String
        get() = categoryEnum.unitLabel

    val pricePer100g: Double
        get() = if (weightGrams > 0) (priceBrl / weightGrams) * 100.0 else 0.0

    val priceDiff: Double?
        get() = previousPriceBrl?.let { priceBrl - it }

    val priceDiffPercent: Double?
        get() = previousPriceBrl?.let { prev ->
            if (prev > 0) ((priceBrl - prev) / prev) * 100.0 else null
        }

    fun formattedPrice(): String {
        return "R$ " + String.format(Locale.getDefault(), "%.2f", priceBrl)
    }

    fun formattedPreviousPrice(): String? {
        return previousPriceBrl?.let { "R$ " + String.format(Locale.getDefault(), "%.2f", it) }
    }

    fun formattedPricePer100g(): String {
        return "R$ " + String.format(Locale.getDefault(), "%.2f", pricePer100g) + "/100$unitLabel"
    }

    fun formattedWeight(): String {
        return "$weightGrams$unitLabel"
    }
}
