package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PestoDatabase
import com.example.data.PestoProduct
import com.example.data.PestoRepository
import com.example.data.ProductCategory
import com.example.data.UpdateSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    PRICE_ASC("Menor Preço"),
    PRICE_DESC("Maior Preço"),
    VALUE_PER_100G("Melhor Custo / 100g/ml"),
    BRAND_ASC("Marca (A-Z)"),
    WEIGHT_DESC("Maior Peso / Volume")
}

enum class ViewMode {
    TABLE,
    CARDS
}

data class PestoUiState(
    val products: List<PestoProduct> = emptyList(),
    val rawProducts: List<PestoProduct> = emptyList(),
    val selectedCategory: ProductCategory = ProductCategory.PESTO,
    val searchQuery: String = "",
    val selectedSupermarket: String? = null,
    val sortOption: SortOption = SortOption.PRICE_ASC,
    val viewMode: ViewMode = ViewMode.TABLE,
    val isUpdating: Boolean = false,
    val updateStatusMessage: String? = null,
    val lastSummary: UpdateSummary? = null,
    val availableSupermarkets: List<String> = emptyList(),
    val lowestPriceProduct: PestoProduct? = null,
    val bestValueProduct: PestoProduct? = null,
    val averagePrice: Double = 0.0,
    val categoryCounts: Map<ProductCategory, Int> = emptyMap()
)

class PestoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PestoRepository

    private val _uiState = MutableStateFlow(PestoUiState())
    val uiState: StateFlow<PestoUiState> = _uiState.asStateFlow()

    init {
        val db = PestoDatabase.getDatabase(application)
        repository = PestoRepository(db.pestoDao())

        viewModelScope.launch {
            repository.ensureInitialData()
        }

        viewModelScope.launch {
            repository.getProductsFlow().collect { allProducts ->
                updateWithProducts(allProducts)
            }
        }
    }

    private fun updateWithProducts(raw: List<PestoProduct>) {
        val current = _uiState.value
        val counts = ProductCategory.values().associateWith { cat ->
            raw.count { it.category == cat.id }
        }

        val filteredAndSorted = filterAndSort(
            raw = raw,
            category = current.selectedCategory,
            query = current.searchQuery,
            market = current.selectedSupermarket,
            sort = current.sortOption
        )

        val categoryRaw = raw.filter { it.category == current.selectedCategory.id }
        val markets = categoryRaw.map { it.supermarket }.distinct().sorted()

        val lowest = if (categoryRaw.isNotEmpty()) categoryRaw.minByOrNull { it.priceBrl } else null
        val bestValue = if (categoryRaw.isNotEmpty()) categoryRaw.minByOrNull { it.pricePer100g } else null
        val avg = if (categoryRaw.isNotEmpty()) categoryRaw.map { it.priceBrl }.average() else 0.0

        _uiState.value = current.copy(
            rawProducts = raw,
            products = filteredAndSorted,
            availableSupermarkets = markets,
            lowestPriceProduct = lowest,
            bestValueProduct = bestValue,
            averagePrice = avg,
            categoryCounts = counts
        )
    }

    private fun filterAndSort(
        raw: List<PestoProduct>,
        category: ProductCategory,
        query: String,
        market: String?,
        sort: SortOption
    ): List<PestoProduct> {
        val inCategory = raw.filter { it.category == category.id }

        val filtered = inCategory.filter { product ->
            val matchesQuery = query.isBlank() ||
                    product.brand.contains(query, ignoreCase = true) ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.origin.contains(query, ignoreCase = true) ||
                    product.supermarket.contains(query, ignoreCase = true)

            val matchesMarket = market == null || market == "Todos" || product.supermarket == market
            matchesQuery && matchesMarket
        }

        return when (sort) {
            SortOption.PRICE_ASC -> filtered.sortedBy { it.priceBrl }
            SortOption.PRICE_DESC -> filtered.sortedByDescending { it.priceBrl }
            SortOption.VALUE_PER_100G -> filtered.sortedBy { it.pricePer100g }
            SortOption.BRAND_ASC -> filtered.sortedWith(compareBy({ it.brand }, { it.priceBrl }))
            SortOption.WEIGHT_DESC -> filtered.sortedByDescending { it.weightGrams }
        }
    }

    fun onCategorySelected(category: ProductCategory) {
        if (_uiState.value.selectedCategory == category) return
        val current = _uiState.value
        val categoryRaw = current.rawProducts.filter { it.category == category.id }
        val markets = categoryRaw.map { it.supermarket }.distinct().sorted()
        val lowest = if (categoryRaw.isNotEmpty()) categoryRaw.minByOrNull { it.priceBrl } else null
        val bestValue = if (categoryRaw.isNotEmpty()) categoryRaw.minByOrNull { it.pricePer100g } else null
        val avg = if (categoryRaw.isNotEmpty()) categoryRaw.map { it.priceBrl }.average() else 0.0

        val filteredAndSorted = filterAndSort(
            raw = current.rawProducts,
            category = category,
            query = current.searchQuery,
            market = null, // Reset market filter on category change
            sort = current.sortOption
        )

        _uiState.value = current.copy(
            selectedCategory = category,
            selectedSupermarket = null,
            products = filteredAndSorted,
            availableSupermarkets = markets,
            lowestPriceProduct = lowest,
            bestValueProduct = bestValue,
            averagePrice = avg
        )
    }

    fun onSearchQueryChanged(query: String) {
        val current = _uiState.value
        val filteredAndSorted = filterAndSort(
            raw = current.rawProducts,
            category = current.selectedCategory,
            query = query,
            market = current.selectedSupermarket,
            sort = current.sortOption
        )
        _uiState.value = current.copy(
            searchQuery = query,
            products = filteredAndSorted
        )
    }

    fun onSupermarketSelected(supermarket: String?) {
        val current = _uiState.value
        val filteredAndSorted = filterAndSort(
            raw = current.rawProducts,
            category = current.selectedCategory,
            query = current.searchQuery,
            market = supermarket,
            sort = current.sortOption
        )
        _uiState.value = current.copy(
            selectedSupermarket = supermarket,
            products = filteredAndSorted
        )
    }

    fun onSortOptionChanged(option: SortOption) {
        val current = _uiState.value
        val filteredAndSorted = filterAndSort(
            raw = current.rawProducts,
            category = current.selectedCategory,
            query = current.searchQuery,
            market = current.selectedSupermarket,
            sort = option
        )
        _uiState.value = current.copy(
            sortOption = option,
            products = filteredAndSorted
        )
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun clearSummary() {
        _uiState.value = _uiState.value.copy(lastSummary = null)
    }

    fun searchAndUpdatePrices() {
        if (_uiState.value.isUpdating) return
        val cat = _uiState.value.selectedCategory
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdating = true,
                updateStatusMessage = "Iniciando varredura para ${cat.title}…"
            )
            try {
                val summary = repository.searchAndUpdatePrices(categoryFilter = cat.id) { msg ->
                    _uiState.value = _uiState.value.copy(updateStatusMessage = msg)
                }
                _uiState.value = _uiState.value.copy(
                    lastSummary = summary,
                    isUpdating = false,
                    updateStatusMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    lastSummary = UpdateSummary(
                        totalUpdated = 0,
                        promoCount = 0,
                        priceDrops = 0,
                        priceIncreases = 0,
                        sourceDescription = "Erro na busca: ${e.message}"
                    ),
                    isUpdating = false,
                    updateStatusMessage = null
                )
            }
        }
    }

    fun addProduct(product: PestoProduct) {
        viewModelScope.launch {
            repository.addProduct(product)
        }
    }

    fun updateProduct(product: PestoProduct) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun deleteProduct(product: PestoProduct) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaultData()
        }
    }
}
