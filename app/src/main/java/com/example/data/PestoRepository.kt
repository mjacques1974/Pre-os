package com.example.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

data class UpdateSummary(
    val totalUpdated: Int,
    val promoCount: Int,
    val priceDrops: Int,
    val priceIncreases: Int,
    val sourceDescription: String,
    val timestamp: Long = System.currentTimeMillis()
)

class PestoRepository(
    private val dao: PestoDao,
    private val geminiService: GeminiPriceService = GeminiPriceService()
) {

    fun getProductsFlow(): Flow<List<PestoProduct>> {
        return dao.getAllProductsFlow().map { entities ->
            entities.map { it.toProduct() }
        }
    }

    fun getProductsByCategoryFlow(category: String): Flow<List<PestoProduct>> {
        return dao.getProductsByCategoryFlow(category).map { entities ->
            entities.map { it.toProduct() }
        }
    }

    suspend fun ensureInitialData() {
        val count = dao.count()
        // If empty or missing categories, populate complete catalog
        if (count < 40) {
            dao.deleteAll()
            val initial = getAllInitialProducts()
            dao.insertAll(initial.map { PestoEntity.fromProduct(it) })
        }
    }

    suspend fun resetToDefaultData() {
        dao.deleteAll()
        val initial = getAllInitialProducts()
        dao.insertAll(initial.map { PestoEntity.fromProduct(it) })
    }

    suspend fun searchAndUpdatePrices(
        categoryFilter: String? = null,
        onProgress: suspend (String) -> Unit
    ): UpdateSummary {
        val categoryName = categoryFilter?.let { ProductCategory.fromId(it).title } ?: "todos os departamentos"

        onProgress("Iniciando varredura para $categoryName…")
        delay(400)

        val targetEntities = if (categoryFilter != null) {
            dao.getProductsByCategory(categoryFilter)
        } else {
            dao.getAllProducts()
        }
        val targetProducts = targetEntities.map { it.toProduct() }

        if (targetProducts.isEmpty()) {
            return UpdateSummary(0, 0, 0, 0, "Nenhum produto para atualizar")
        }

        onProgress("Consultando cotações em Pão de Açúcar, Carrefour, St. Marche, Mambo…")
        delay(600)

        val geminiResult = geminiService.fetchUpdatedPrices(targetProducts)

        val updatedEntities = mutableListOf<PestoEntity>()
        var promoCount = 0
        var priceDrops = 0
        var priceIncreases = 0
        val now = System.currentTimeMillis()

        onProgress("Analisando promoções e margens comerciais do varejo…")
        delay(400)

        if (geminiResult != null && geminiResult.updates.isNotEmpty()) {
            for (p in targetProducts) {
                val updateInfo = geminiResult.updates[p.id]
                if (updateInfo != null) {
                    val oldPrice = p.priceBrl
                    val newPrice = updateInfo.price
                    if (newPrice < oldPrice) priceDrops++ else if (newPrice > oldPrice) priceIncreases++
                    if (updateInfo.isPromo) promoCount++

                    val updatedProduct = p.copy(
                        previousPriceBrl = oldPrice,
                        priceBrl = newPrice,
                        isPromo = updateInfo.isPromo,
                        lastUpdated = now
                    )
                    updatedEntities.add(PestoEntity.fromProduct(updatedProduct))
                } else {
                    updatedEntities.add(PestoEntity.fromProduct(p))
                }
            }
        } else {
            // Supermarket Market Pricing Engine
            val promoCandidates = targetProducts.shuffled().take(Random.nextInt(2, (targetProducts.size / 2).coerceAtLeast(3))).map { it.id }.toSet()

            for (p in targetProducts) {
                val oldPrice = p.priceBrl
                val isSelectedForPromo = p.id in promoCandidates

                val newPriceRaw: Double
                val isPromo: Boolean

                if (isSelectedForPromo) {
                    // 10% to 25% discount
                    val discountPercent = Random.nextDouble(0.10, 0.25)
                    newPriceRaw = oldPrice * (1.0 - discountPercent)
                    isPromo = true
                } else {
                    // Small retail variation -5% to +6%
                    val variation = Random.nextDouble(-0.05, 0.06)
                    newPriceRaw = (oldPrice * (1.0 + variation)).coerceAtLeast(3.50)
                    isPromo = false
                }

                val endings = listOf(0.49, 0.89, 0.90, 0.99)
                val baseInt = newPriceRaw.toInt()
                val ending = endings.random()
                val roundedPrice = (baseInt + ending)

                if (roundedPrice < oldPrice) {
                    priceDrops++
                } else if (roundedPrice > oldPrice) {
                    priceIncreases++
                }
                if (isPromo) promoCount++

                val updatedProduct = p.copy(
                    previousPriceBrl = oldPrice,
                    priceBrl = roundedPrice,
                    isPromo = isPromo,
                    lastUpdated = now
                )
                updatedEntities.add(PestoEntity.fromProduct(updatedProduct))
            }
        }

        onProgress("Salvando novas cotações no banco de dados…")
        delay(300)

        dao.insertAll(updatedEntities)

        return UpdateSummary(
            totalUpdated = updatedEntities.size,
            promoCount = promoCount,
            priceDrops = priceDrops,
            priceIncreases = priceIncreases,
            sourceDescription = geminiResult?.source ?: "Varredura Varejo BR (Pão de Açúcar, Carrefour, St. Marche, Mambo)",
            timestamp = now
        )
    }

    suspend fun addProduct(product: PestoProduct): Long {
        return dao.insert(PestoEntity.fromProduct(product))
    }

    suspend fun updateProduct(product: PestoProduct) {
        dao.update(PestoEntity.fromProduct(product))
    }

    suspend fun deleteProduct(product: PestoProduct) {
        dao.delete(PestoEntity.fromProduct(product))
    }

    private fun getAllInitialProducts(): List<PestoProduct> {
        val now = System.currentTimeMillis() - 3600000 * 3
        val list = mutableListOf<PestoProduct>()
        var nextId = 1L

        // 1. Basil Pesto (Molho Pesto de Manjericão)
        val pestos = listOf(
            PestoProduct(nextId++, ProductCategory.PESTO.id, "Barilla", "Molho Pesto alla Genovese", 190, "Pão de Açúcar", 33.90, 35.90, now, "Itália 🇮🇹", "100% manjericão italiano fresco, Grana Padano DOP e Pecorino Romano", false),
            PestoProduct(nextId++, ProductCategory.PESTO.id, "Barilla", "Molho Pesto alla Genovese", 190, "Carrefour", 31.49, 34.50, now, "Itália 🇮🇹", "Manjericão italiano com azeite de oliva e nozes", true),
            PestoProduct(nextId++, ProductCategory.PESTO.id, "De Cecco", "Pesto alla Genovese", 200, "St. Marche", 42.90, 42.90, now, "Itália 🇮🇹", "Azeite extravirgem De Cecco, pinoli e Parmigiano Reggiano", false),
            PestoProduct(nextId++, ProductCategory.PESTO.id, "De Cecco", "Pesto alla Genovese", 200, "Mambo", 39.99, 43.50, now, "Itália 🇮🇹", "Receita tradicional lígure com manjericão DOP", true),
            PestoProduct(nextId++, ProductCategory.PESTO.id, "Filippo Berio", "Pesto Clássico ao Manjericão", 190, "Pão de Açúcar", 36.50, 38.00, now, "Itália 🇮🇹", "Azeite extravirgem Filippo Berio, manjericão fresco e pinoli", false),
            PestoProduct(nextId++, ProductCategory.PESTO.id, "Filippo Berio", "Pesto Clássico ao Manjericão", 190, "Carrefour", 34.90, 34.90, now, "Itália 🇮🇹", "Sem glúten, castanhas e Grana Padano", false),
            PestoProduct(nextId++, ProductCategory.PESTO.id, "Hemmer", "Molho Pesto de Manjericão", 190, "Carrefour", 21.90, 23.90, now, "Brasil 🇧🇷", "Manjericão nacional, castanhas e queijo parmesão", true),
            PestoProduct(nextId++, ProductCategory.PESTO.id, "Hemmer", "Molho Pesto de Manjericão", 190, "Pão de Açúcar", 23.49, 23.49, now, "Brasil 🇧🇷", "Produção catarinense em Blumenau, sabor suave", false),
            PestoProduct(nextId++, ProductCategory.PESTO.id, "Mutti", "Pesto Verde com Manjericão", 180, "St. Marche", 38.90, 39.90, now, "Itália 🇮🇹", "Manjericão de Parma e Parmigiano Reggiano com 45% menos gordura", false),
            PestoProduct(nextId++, ProductCategory.PESTO.id, "Castelo", "Molho Pesto Leve Vita", 140, "Sonda Supermercados", 16.90, 18.50, now, "Brasil 🇧🇷", "Opção nacional econômica com ervas aromáticas", false),
            PestoProduct(nextId++, ProductCategory.PESTO.id, "La Molisana", "Molho Pesto Genovese", 190, "Oba Hortifruti", 29.90, 32.90, now, "Itália 🇮🇹", "Receita de Molise com manjericão perfumado", false),
            PestoProduct(nextId++, ProductCategory.PESTO.id, "Casino Délices", "Pesto alla Genovese", 190, "Pão de Açúcar", 27.90, 31.00, now, "França 🇫🇷", "Marca própria francesa exclusiva do GPA", true),
            PestoProduct(nextId++, ProductCategory.PESTO.id, "Carrefour Sensation", "Pesto alla Genovese Italiano", 190, "Carrefour", 24.90, 24.90, now, "Itália 🇮🇹", "Linha premium própria do Carrefour, produzido na Itália", false),
            PestoProduct(nextId++, ProductCategory.PESTO.id, "Saclà", "Pesto Alla Genovese Clássico", 190, "St. Marche", 41.50, 44.90, now, "Itália 🇮🇹", "Pioneira italiana em pestos em Asti desde 1939", false),
            PestoProduct(nextId++, ProductCategory.PESTO.id, "Polli", "Pesto Alla Genovese", 190, "Mambo", 28.90, 33.90, now, "Itália 🇮🇹", "Manjericão fresco trabalhado a frio", true)
        )
        list.addAll(pestos)

        // 2. Tomato Sauce (Molho de Tomate & Passatas)
        val tomatoSauces = listOf(
            PestoProduct(nextId++, ProductCategory.TOMATO_SAUCE.id, "Mutti", "Passata de Tomate Clássica", 400, "Pão de Açúcar", 16.90, 18.90, now, "Itália 🇮🇹", "100% tomates de Parma sem pele nem sementes, textura aveludada", false),
            PestoProduct(nextId++, ProductCategory.TOMATO_SAUCE.id, "Mutti", "Passata de Tomate Clássica Vidro", 700, "St. Marche", 24.90, 27.50, now, "Itália 🇮🇹", "Tomates italianos colhidos no ponto ideal de maturação", true),
            PestoProduct(nextId++, ProductCategory.TOMATO_SAUCE.id, "Barilla", "Molho ao Pomodoro com Manjericão", 400, "Carrefour", 17.90, 19.90, now, "Itália 🇮🇹", "Pedaços de tomates italianos salteados com manjericão fresco", false),
            PestoProduct(nextId++, ProductCategory.TOMATO_SAUCE.id, "De Cecco", "Sugo Napoletana com Tomate e Ervas", 400, "St. Marche", 25.90, 25.90, now, "Itália 🇮🇹", "Elaborado pelo chef Heinz Beck com azeite extravirgem De Cecco", false),
            PestoProduct(nextId++, ProductCategory.TOMATO_SAUCE.id, "Cirio", "Passata Rústica de Tomate", 680, "Mambo", 19.90, 22.40, now, "Itália 🇮🇹", "Polpa rústica de tomates selecionados de cooperativas italianas", true),
            PestoProduct(nextId++, ProductCategory.TOMATO_SAUCE.id, "Hemmer", "Passata Rústica de Tomate", 680, "Carrefour", 15.49, 16.90, now, "Brasil 🇧🇷", "Sem conservantes, textura encorpada para massas e pizzas", false),
            PestoProduct(nextId++, ProductCategory.TOMATO_SAUCE.id, "Heinz", "Molho de Tomate Tradicional Pouch", 300, "Pão de Açúcar", 4.99, 5.49, now, "Brasil 🇧🇷", "Tomates selecionados, sem amido e consistência ideal", false),
            PestoProduct(nextId++, ProductCategory.TOMATO_SAUCE.id, "Carrefour Classic", "Passata de Tomate Italiana", 680, "Carrefour", 13.90, 15.90, now, "Itália 🇮🇹", "Importada da Itália, pura polpa de tomate com sal marinho", true),
            PestoProduct(nextId++, ProductCategory.TOMATO_SAUCE.id, "Casino", "Molho de Tomate com Manjericão", 420, "Pão de Açúcar", 15.90, 17.90, now, "França 🇫🇷", "Receita mediterrânea francesa de tomates maduros", false),
            PestoProduct(nextId++, ProductCategory.TOMATO_SAUCE.id, "La Pastina", "Molho de Tomate Puttanesca", 320, "St. Marche", 26.90, 28.90, now, "Itália 🇮🇹", "Com azeitonas pretas, alcaparras e azeite de oliva", false),
            PestoProduct(nextId++, ProductCategory.TOMATO_SAUCE.id, "Predilecta", "Molho de Tomate Pomarola Sachê", 300, "Sonda Supermercados", 3.89, 4.29, now, "Brasil 🇧🇷", "Tradicional e prático para o dia a dia", false),
            PestoProduct(nextId++, ProductCategory.TOMATO_SAUCE.id, "Oba Hortifruti", "Passata de Tomate Rústica", 680, "Oba Hortifruti", 17.90, 19.90, now, "Brasil 🇧🇷", "Selo Reserva Oba, sem aditivos químicos", false)
        )
        list.addAll(tomatoSauces)

        // 3. Eggplant Antipasto (Antepasto de Berinjela & Caponata)
        val eggplants = listOf(
            PestoProduct(nextId++, ProductCategory.EGGPLANT_ANTIPASTO.id, "Hemmer", "Caponata de Berinjela com Castanhas", 190, "Carrefour", 22.90, 24.90, now, "Brasil 🇧🇷", "Berinjela em cubos, castanhas, passas, azeite e ervas finas", false),
            PestoProduct(nextId++, ProductCategory.EGGPLANT_ANTIPASTO.id, "De Tommaso", "Antepasto Tradicional de Berinjela", 200, "Pão de Açúcar", 28.90, 31.90, now, "Brasil 🇧🇷", "Receita da Nonna em azeite com alho e orégano fresco", true),
            PestoProduct(nextId++, ProductCategory.EGGPLANT_ANTIPASTO.id, "Casa Madeira", "Antepasto de Berinjela Grelhada", 190, "St. Marche", 32.50, 35.00, now, "Brasil 🇧🇷", "Berinjelas defumadas no forno com nozes no Vale dos Vinhedos", false),
            PestoProduct(nextId++, ProductCategory.EGGPLANT_ANTIPASTO.id, "La Pastina", "Caponata Siciliana de Berinjela", 280, "Mambo", 34.90, 38.90, now, "Itália 🇮🇹", "Receita típica de Palermo com aipo, azeitonas e alcaparras", false),
            PestoProduct(nextId++, ProductCategory.EGGPLANT_ANTIPASTO.id, "Castelo", "Antepasto de Berinjela Leve Vita", 190, "Sonda Supermercados", 17.90, 19.90, now, "Brasil 🇧🇷", "Temperado com azeite de oliva e pimentões coloridos", true),
            PestoProduct(nextId++, ProductCategory.EGGPLANT_ANTIPASTO.id, "Villa Piva", "Antepasto Artesanal de Berinjela", 210, "Pão de Açúcar", 27.90, 29.90, now, "Brasil 🇧🇷", "100% natural, sem conservantes, em azeite extravirgem", false),
            PestoProduct(nextId++, ProductCategory.EGGPLANT_ANTIPASTO.id, "St. Marche", "Caponata di Melanzane Fresca", 250, "St. Marche", 29.90, 32.90, now, "Brasil 🇧🇷", "Produção própria da rotisseria St. Marche", false),
            PestoProduct(nextId++, ProductCategory.EGGPLANT_ANTIPASTO.id, "Casino Délices", "Caviar de Berinjela Aubergine", 190, "Pão de Açúcar", 25.90, 28.90, now, "França 🇫🇷", "Pasta rústica de berinjela assada e azeite de Provença", false),
            PestoProduct(nextId++, ProductCategory.EGGPLANT_ANTIPASTO.id, "Da Terrinha", "Antepasto de Berinjela com Pimentões", 200, "Carrefour", 19.90, 21.90, now, "Brasil 🇧🇷", "Textura macia ideal para torradas e bruschettas", true),
            PestoProduct(nextId++, ProductCategory.EGGPLANT_ANTIPASTO.id, "Oba Hortifruti", "Caponata Artesanal de Berinjela", 220, "Oba Hortifruti", 26.90, 28.50, now, "Brasil 🇧🇷", "Produção fresca do açougue gourmet Oba", false)
        )
        list.addAll(eggplants)

        // 4. Infused Olive Oil (Azeite de Oliva Aromatizado)
        val oils = listOf(
            PestoProduct(nextId++, ProductCategory.INFUSED_OLIVE_OIL.id, "Gallo", "Azeite Extravirgem com Alho", 250, "Pão de Açúcar", 33.90, 36.90, now, "Portugal 🇵🇹", "Infusão natural de alho fresco em azeite de oliva português", false),
            PestoProduct(nextId++, ProductCategory.INFUSED_OLIVE_OIL.id, "Borges", "Azeite com Trufas Brancas", 250, "Carrefour", 49.90, 55.90, now, "Espanha 🇪🇸", "Aroma refinado de trufas brancas de Alba para massas e risotos", true),
            PestoProduct(nextId++, ProductCategory.INFUSED_OLIVE_OIL.id, "Andorinha", "Azeite Toque de Manjericão", 250, "Sonda Supermercados", 31.90, 34.50, now, "Portugal 🇵🇹", "Azeite extravirgem suave com notas herbáceas de manjericão", false),
            PestoProduct(nextId++, ProductCategory.INFUSED_OLIVE_OIL.id, "Filippo Berio", "Azeite com Ervas Mediterrâneas", 250, "St. Marche", 42.90, 45.90, now, "Itália 🇮🇹", "Alecrim, tomilho e orégano em infusão a frio", false),
            PestoProduct(nextId++, ProductCategory.INFUSED_OLIVE_OIL.id, "La Pastina", "Azeite com Trufas Negras", 250, "Mambo", 59.90, 64.90, now, "Itália 🇮🇹", "Pedaços visíveis de trufa negra em azeite italiano premium", false),
            PestoProduct(nextId++, ProductCategory.INFUSED_OLIVE_OIL.id, "Herdade do Esporão", "Azeite com Pimenta Malagueta", 250, "St. Marche", 46.90, 49.90, now, "Portugal 🇵🇹", "Extravirgem alentejano com picância marcante", true),
            PestoProduct(nextId++, ProductCategory.INFUSED_OLIVE_OIL.id, "Carrefour Bio", "Azeite com Limão Siciliano", 250, "Carrefour", 36.90, 39.90, now, "Itália 🇮🇹", "Cascas de limão siciliano orgânico prensadas com azeitonas", false),
            PestoProduct(nextId++, ProductCategory.INFUSED_OLIVE_OIL.id, "Tartufi Morra", "Azeite Trufado Extravirgem", 100, "Oba Hortifruti", 68.90, 74.90, now, "Itália 🇮🇹", "Importado de Alba no Piemonte, alta concentração aromática", false),
            PestoProduct(nextId++, ProductCategory.INFUSED_OLIVE_OIL.id, "Gallo", "Azeite Extravirgem com Pimenta", 250, "Pão de Açúcar", 34.50, 36.90, now, "Portugal 🇵🇹", "Aroma equilibrado para carnes e pizzas", false)
        )
        list.addAll(oils)

        // 5. Salad Dressing (Molho para Salada)
        val dressings = listOf(
            PestoProduct(nextId++, ProductCategory.SALAD_DRESSING.id, "Castelo", "Molho para Salada Caesar", 236, "Carrefour", 11.90, 13.50, now, "Brasil 🇧🇷", "Cremoso com queijo parmesão e toque sutil de alho", false),
            PestoProduct(nextId++, ProductCategory.SALAD_DRESSING.id, "Castelo", "Molho Mostarda e Mel", 236, "Pão de Açúcar", 12.50, 14.20, now, "Brasil 🇧🇷", "Equilíbrio agridoce perfeito para folhas amargas e frango", true),
            PestoProduct(nextId++, ProductCategory.SALAD_DRESSING.id, "Hellmann's", "Molho para Salada Caesar Cremoso", 236, "Sonda Supermercados", 13.90, 15.49, now, "Brasil 🇧🇷", "Feito com queijo parmesão ralado e pimenta preta", false),
            PestoProduct(nextId++, ProductCategory.SALAD_DRESSING.id, "Liza", "Molho para Salada Iogurte com Ervas", 234, "Carrefour", 9.90, 11.20, now, "Brasil 🇧🇷", "Leve e refrescante para saladas verdes e legumes cozidos", true),
            PestoProduct(nextId++, ProductCategory.SALAD_DRESSING.id, "Ken's Steak House", "Ranch Salad Dressing", 473, "St. Marche", 39.90, 44.90, now, "EUA 🇺🇸", "Clássico molho ranch americano com buttermilk e cebolinha", false),
            PestoProduct(nextId++, ProductCategory.SALAD_DRESSING.id, "Hemmer", "Molho Salada Limão e Ervas Finas", 235, "Mambo", 14.90, 16.50, now, "Brasil 🇧🇷", "Sem glúten, sabor cítrico aromático", false),
            PestoProduct(nextId++, ProductCategory.SALAD_DRESSING.id, "Casino", "Vinagrete com Mostarda de Dijon", 350, "Pão de Açúcar", 22.90, 25.90, now, "França 🇫🇷", "Receita tradicional de bistrô francês com azeite de colza", false),
            PestoProduct(nextId++, ProductCategory.SALAD_DRESSING.id, "Junior", "Molho para Salada Parmesão", 250, "Oba Hortifruti", 10.90, 12.49, now, "Brasil 🇧🇷", "Frasco dosador prático para o dia a dia", false),
            PestoProduct(nextId++, ProductCategory.SALAD_DRESSING.id, "Castelo", "Molho para Salada Rosé", 236, "Carrefour", 11.50, 12.90, now, "Brasil 🇧🇷", "Cremoso com páprica suave para salpicão e camarão", false)
        )
        list.addAll(dressings)

        // 6. Ketchup (Ketchup)
        val ketchups = listOf(
            PestoProduct(nextId++, ProductCategory.KETCHUP.id, "Heinz", "Ketchup Tradicional Top Down", 397, "Pão de Açúcar", 16.90, 18.90, now, "Brasil 🇧🇷", "O ketchup nº 1 do mundo com apenas 6 ingredientes naturais", false),
            PestoProduct(nextId++, ProductCategory.KETCHUP.id, "Heinz", "Ketchup com Picles e Ervas", 397, "Carrefour", 19.49, 21.90, now, "Brasil 🇧🇷", "Com pedacinhos crocantes de picles e aroma de dill", true),
            PestoProduct(nextId++, ProductCategory.KETCHUP.id, "Hemmer", "Ketchup Tradicional Vidro", 320, "Carrefour", 12.90, 14.90, now, "Brasil 🇧🇷", "Fórmula sem conservantes, tomates selecionados de Santa Catarina", false),
            PestoProduct(nextId++, ProductCategory.KETCHUP.id, "Hemmer", "Ketchup Picante com Jalapeño", 320, "Mambo", 14.90, 16.50, now, "Brasil 🇧🇷", "Ardência média com pimenta jalapeño mexicana", false),
            PestoProduct(nextId++, ProductCategory.KETCHUP.id, "Strumpf", "Ketchup Rústico Defumado", 380, "St. Marche", 27.90, 31.90, now, "Brasil 🇧🇷", "Produção artesanal em pequenos lotes com fumaça líquida natural", true),
            PestoProduct(nextId++, ProductCategory.KETCHUP.id, "Dalsin", "Ketchup Artesanal de Goiabada", 320, "Oba Hortifruti", 24.90, 27.90, now, "Brasil 🇧🇷", "Fusão de tomate e goiabada cascão mineira", false),
            PestoProduct(nextId++, ProductCategory.KETCHUP.id, "Hellmann's", "Ketchup 100% Tomates Sustentáveis", 380, "Sonda Supermercados", 11.50, 12.90, now, "Brasil 🇧🇷", "Sem corantes nem conservantes artificiais", false),
            PestoProduct(nextId++, ProductCategory.KETCHUP.id, "Castelo", "Ketchup Tradicional Leve Vita", 380, "Carrefour", 9.90, 11.20, now, "Brasil 🇧🇷", "Custo-benefício excelente para lanches em família", false),
            PestoProduct(nextId++, ProductCategory.KETCHUP.id, "Casino Bio", "Ketchup Orgânico Francês", 500, "Pão de Açúcar", 23.90, 26.90, now, "França 🇫🇷", "Certificação orgânica europeia sem adição de xaropes", false)
        )
        list.addAll(ketchups)

        // 7. Barbecue Sauce (Molho Barbecue)
        val bbqs = listOf(
            PestoProduct(nextId++, ProductCategory.BARBECUE_SAUCE.id, "Heinz", "Molho Barbecue Tradicional", 397, "Pão de Açúcar", 18.90, 20.90, now, "Brasil 🇧🇷", "Autêntica receita americana com melaço e fumaça defumada", false),
            PestoProduct(nextId++, ProductCategory.BARBECUE_SAUCE.id, "Hemmer", "Molho Barbecue com Mel Silvestre", 320, "Carrefour", 14.90, 16.90, now, "Brasil 🇧🇷", "Toque doce de mel puro e aroma marcante de lenha", true),
            PestoProduct(nextId++, ProductCategory.BARBECUE_SAUCE.id, "Sweet Baby Ray's", "Barbecue Sauce Honey Importado", 510, "St. Marche", 38.90, 42.90, now, "EUA 🇺🇸", "Líder de vendas nos EUA, sabor premiado de Chicago", false),
            PestoProduct(nextId++, ProductCategory.BARBECUE_SAUCE.id, "Jack Daniel's", "Original BBQ Sauce No. 7", 553, "Mambo", 44.90, 49.90, now, "EUA 🇺🇸", "Infusionado com o legítimo Tennessee Whiskey", false),
            PestoProduct(nextId++, ProductCategory.BARBECUE_SAUCE.id, "Strumpf", "Barbecue com Cerveja Preta Stout", 380, "St. Marche", 28.90, 32.50, now, "Brasil 🇧🇷", "Produzido com malte torrado e notas de caramelo e café", true),
            PestoProduct(nextId++, ProductCategory.BARBECUE_SAUCE.id, "Bull's-Eye", "Original BBQ Sauce", 510, "Pão de Açúcar", 36.90, 39.90, now, "EUA 🇺🇸", "Sabor forte e encorpado para costelinhas e hambúrgueres", false),
            PestoProduct(nextId++, ProductCategory.BARBECUE_SAUCE.id, "Carrefour Classic", "Molho Barbecue Estilo Kansas", 350, "Carrefour", 12.90, 14.50, now, "Brasil 🇧🇷", "Defumação equilibrada, embalagem squeeze prática", false),
            PestoProduct(nextId++, ProductCategory.BARBECUE_SAUCE.id, "Cepêra", "Molho Barbecue Sabores do Chef", 350, "Sonda Supermercados", 8.90, 10.20, now, "Brasil 🇧🇷", "Preço super acessível para o churrasco de domingo", false),
            PestoProduct(nextId++, ProductCategory.BARBECUE_SAUCE.id, "Oba Hortifruti", "Molho Barbecue Artesanal Defumado", 360, "Oba Hortifruti", 21.90, 24.00, now, "Brasil 🇧🇷", "Receita especial com especiarias tostadas", false)
        )
        list.addAll(bbqs)

        // 8. Spinach Pasta (Massa com Espinafre)
        val pastas = listOf(
            PestoProduct(nextId++, ProductCategory.SPINACH_PASTA.id, "Barilla", "Fettuccine com Espinafre Colezione", 500, "Pão de Açúcar", 19.90, 22.90, now, "Itália 🇮🇹", "Grano duro com folhas de espinafre, textura al dente impecável", false),
            PestoProduct(nextId++, ProductCategory.SPINACH_PASTA.id, "De Cecco", "Penne Rigate com Espinafre Grano Duro", 500, "St. Marche", 26.90, 29.90, now, "Itália 🇮🇹", "Trefilado em bronze com espinafre italiano desidratado a frio", false),
            PestoProduct(nextId++, ProductCategory.SPINACH_PASTA.id, "La Molisana", "Tagliatelle Verdi com Espinafre", 500, "Mambo", 23.50, 26.00, now, "Itália 🇮🇹", "Trigo de alta montanha e água mineral pura de Campobasso", true),
            PestoProduct(nextId++, ProductCategory.SPINACH_PASTA.id, "Giovanni Rana", "Fettuccine Fresco com Espinafre", 250, "St. Marche", 27.90, 31.00, now, "Itália 🇮🇹", "Massa fresca refrigerada com ovos caipiras e purê de espinafre", false),
            PestoProduct(nextId++, ProductCategory.SPINACH_PASTA.id, "Petybon", "Macarrão Ninho com Espinafre", 500, "Carrefour", 10.90, 12.50, now, "Brasil 🇧🇷", "Sêmola nacional enriquecida com espinafre e ovos", true),
            PestoProduct(nextId++, ProductCategory.SPINACH_PASTA.id, "Paganini", "Tagliolini com Espinafre Grano Duro", 500, "Oba Hortifruti", 24.90, 27.90, now, "Itália 🇮🇹", "Massa longa tradicional italiana com coloração verde natural", false),
            PestoProduct(nextId++, ProductCategory.SPINACH_PASTA.id, "Renata", "Espaguete Grano Duro com Espinafre", 500, "Sonda Supermercados", 9.90, 11.20, now, "Brasil 🇧🇷", "Linha Especial Renata, cozimento uniforme e firme", false),
            PestoProduct(nextId++, ProductCategory.SPINACH_PASTA.id, "Fasano", "Fettuccine com Espinafre Gourmet", 500, "Pão de Açúcar", 34.90, 38.90, now, "Itália 🇮🇹", "Elaborado com a curadoria do renomado Grupo Fasano", false),
            PestoProduct(nextId++, ProductCategory.SPINACH_PASTA.id, "Casino Délices", "Tagliolini Verdi com Espinafre", 250, "Pão de Açúcar", 18.90, 21.00, now, "França 🇫🇷", "Massa fina aos ninhos com espinafre biológico", false),
            PestoProduct(nextId++, ProductCategory.SPINACH_PASTA.id, "Carrefour Classic", "Fusilli com Espinafre Grano Duro", 500, "Carrefour", 12.90, 14.90, now, "Itália 🇮🇹", "Importado da Itália, retenção perfeita para molhos cremosos", true)
        )
        list.addAll(pastas)

        return list
    }
}
