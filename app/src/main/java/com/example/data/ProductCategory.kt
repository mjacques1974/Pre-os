package com.example.data

enum class ProductCategory(
    val id: String,
    val title: String,
    val shortName: String,
    val emoji: String,
    val description: String,
    val unitLabel: String = "g"
) {
    PESTO(
        id = "PESTO",
        title = "Molho Pesto de Manjericão",
        shortName = "Pesto",
        emoji = "🌿",
        description = "Pestos alla Genovese italianos e receitas nacionais em frasco",
        unitLabel = "g"
    ),
    TOMATO_SAUCE(
        id = "TOMATO_SAUCE",
        title = "Molho de Tomate & Passatas",
        shortName = "Tomate",
        emoji = "🍅",
        description = "Passatas italianas rústicas, sugos ao pomodoro e molhos temperados",
        unitLabel = "g"
    ),
    EGGPLANT_ANTIPASTO(
        id = "EGGPLANT_ANTIPASTO",
        title = "Antepasto de Berinjela & Caponata",
        shortName = "Berinjela",
        emoji = "🍆",
        description = "Caponatas sicilianas, antepastos grelhados com azeite e conservas",
        unitLabel = "g"
    ),
    INFUSED_OLIVE_OIL(
        id = "INFUSED_OLIVE_OIL",
        title = "Azeite de Oliva Aromatizado",
        shortName = "Azeite",
        emoji = "🫒",
        description = "Azeites extravirgem trufados, com alho, pimenta, alecrim e ervas",
        unitLabel = "ml"
    ),
    SALAD_DRESSING(
        id = "SALAD_DRESSING",
        title = "Molho para Salada",
        shortName = "Salada",
        emoji = "🥗",
        description = "Molhos Caesar, mostarda e mel, iogurte, vinagretes e ervas finas",
        unitLabel = "ml"
    ),
    KETCHUP(
        id = "KETCHUP",
        title = "Ketchup",
        shortName = "Ketchup",
        emoji = "🥫",
        description = "Ketchups rústicos, tradicionais, defumados e com pimenta jalapeño",
        unitLabel = "g"
    ),
    BARBECUE_SAUCE(
        id = "BARBECUE_SAUCE",
        title = "Molho Barbecue",
        shortName = "Barbecue",
        emoji = "🍖",
        description = "Molhos barbecue artesanais defumados, com mel e estilo americano",
        unitLabel = "g"
    ),
    SPINACH_PASTA(
        id = "SPINACH_PASTA",
        title = "Massa com Espinafre",
        shortName = "Massa Verde",
        emoji = "🍝",
        description = "Fettuccine, tagliatelle, penne e ninhos grano duro com espinafre fresco",
        unitLabel = "g"
    );

    companion object {
        fun fromId(id: String): ProductCategory {
            return values().firstOrNull { it.id == id } ?: PESTO
        }
    }
}
