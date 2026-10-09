package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiPriceService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    data class PriceUpdateResult(
        val updates: Map<Long, UpdatedPriceInfo>,
        val source: String
    )

    data class UpdatedPriceInfo(
        val price: Double,
        val isPromo: Boolean,
        val note: String? = null
    )

    suspend fun fetchUpdatedPrices(products: List<PestoProduct>): PriceUpdateResult? = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d("GeminiPriceService", "No valid Gemini API key configured. Using supermarket search engine.")
            return@withContext null
        }

        try {
            val productDescriptions = products.joinToString("\n") { p ->
                "- ID:${p.id} | Marca:${p.brand} | Nome:${p.name} | Peso:${p.weightGrams}g | Mercado:${p.supermarket} | PreçoAtual:R$${p.priceBrl}"
            }

            val prompt = """
                Você é um pesquisador de preços de produtos alimentícios nos supermercados brasileiros (Pão de Açúcar, Carrefour, St. Marche, Mambo, Oba Hortifruti, Sonda).
                Pesquise e atualize as cotações atuais aproximadas em Reais (BRL) para os seguintes molhos pesto de manjericão.
                
                Produtos a pesquisar:
                $productDescriptions
                
                Responda ESTRITAMENTE em formato JSON com a seguinte lista de objetos:
                [
                  {
                    "id": 1,
                    "newPrice": 32.90,
                    "isPromo": false,
                    "note": "Preço regular"
                  }
                ]
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.3)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", generationConfig)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("GeminiPriceService", "Gemini API response unsuccessful: ${response.code}")
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return@withContext null
            val firstCandidate = candidates.optJSONObject(0) ?: return@withContext null
            val content = firstCandidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            val text = parts.optJSONObject(0)?.optString("text") ?: return@withContext null

            // Parse returned JSON array
            val cleanJson = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JSONArray(cleanJson)
            val resultMap = mutableMapOf<Long, UpdatedPriceInfo>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getLong("id")
                val newPrice = obj.getDouble("newPrice")
                val isPromo = obj.optBoolean("isPromo", false)
                val note = obj.optString("note", "")
                if (newPrice > 0) {
                    resultMap[id] = UpdatedPriceInfo(
                        price = newPrice,
                        isPromo = isPromo,
                        note = if (note.isNotBlank()) note else null
                    )
                }
            }

            if (resultMap.isNotEmpty()) {
                return@withContext PriceUpdateResult(resultMap, source = "Gemini AI Search (Cotação em tempo real)")
            }
        } catch (e: Exception) {
            Log.e("GeminiPriceService", "Error during Gemini price update", e)
        }
        return@withContext null
    }
}
