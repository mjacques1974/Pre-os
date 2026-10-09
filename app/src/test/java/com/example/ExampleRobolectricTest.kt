package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.PestoDatabase
import com.example.data.PestoProduct
import com.example.data.PestoRepository
import com.example.data.ProductCategory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: PestoDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PestoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PestoPreço", appName)
    }

    @Test
    fun `calculate price per 100g correctly`() {
        val pesto = PestoProduct(
            id = 1,
            category = ProductCategory.PESTO.id,
            brand = "Barilla",
            name = "Pesto alla Genovese",
            weightGrams = 190,
            supermarket = "Carrefour",
            priceBrl = 31.49,
            previousPriceBrl = 34.50
        )

        val expectedPer100g = (31.49 / 190.0) * 100.0
        assertEquals(expectedPer100g, pesto.pricePer100g, 0.01)

        val diff = pesto.priceDiff
        assertNotNull(diff)
        assertTrue(diff!! < 0)
    }

    @Test
    fun `database inserts and queries all 8 categories`() = runBlocking {
        val repo = PestoRepository(db.pestoDao())
        repo.ensureInitialData()

        val all = db.pestoDao().getAllProducts()
        assertTrue(all.size >= 50)

        // Verify each requested category is present
        val categories = listOf(
            ProductCategory.PESTO.id,
            ProductCategory.TOMATO_SAUCE.id,
            ProductCategory.EGGPLANT_ANTIPASTO.id,
            ProductCategory.INFUSED_OLIVE_OIL.id,
            ProductCategory.SALAD_DRESSING.id,
            ProductCategory.KETCHUP.id,
            ProductCategory.BARBECUE_SAUCE.id,
            ProductCategory.SPINACH_PASTA.id
        )

        for (cat in categories) {
            val inCat = db.pestoDao().getProductsByCategory(cat)
            assertTrue("Category $cat should have items", inCat.isNotEmpty())
        }
    }

    @Test
    fun `price search and update updates category prices`() = runBlocking {
        val repo = PestoRepository(db.pestoDao())
        repo.ensureInitialData()

        val summary = repo.searchAndUpdatePrices(ProductCategory.TOMATO_SAUCE.id) { /* progress */ }
        assertTrue(summary.totalUpdated > 0)

        val tomatoList = db.pestoDao().getProductsByCategory(ProductCategory.TOMATO_SAUCE.id)
        assertTrue(tomatoList.any { it.previousPriceBrl != null })
    }
}
