package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PestoProduct
import com.example.ui.components.AddEditProductDialog
import com.example.ui.components.CategoryTabBar
import com.example.ui.components.PestoCard
import com.example.ui.components.PestoStatsHeader
import com.example.ui.components.PestoTable
import com.example.ui.components.ProductDetailDialog
import com.example.ui.components.UpdateSummaryDialog
import com.example.ui.theme.BasilGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PestoScreen(
    viewModel: PestoViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var productToEdit by remember { mutableStateOf<PestoProduct?>(null) }
    var productForDetail by remember { mutableStateOf<PestoProduct?>(null) }
    var isAddingProduct by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Infinite rotation for search/update icon when loading
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo_pagueti),
                                    contentDescription = "Pagueti Logo",
                                    modifier = Modifier
                                        .height(26.dp)
                                        .widthIn(max = 110.dp)
                                        .testTag("app_bar_pagueti_logo"),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Pagueti",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Comparador de Preços em Supermercados",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.resetToDefaults() },
                            modifier = Modifier.testTag("btn_reset_defaults")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Restaurar Catálogo Original",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(
                            onClick = { isAddingProduct = true },
                            modifier = Modifier.testTag("btn_add_product_top")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Adicionar Produto",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BasilGreenPrimary,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )

                // Category Tabs Bar
                CategoryTabBar(
                    selectedCategory = uiState.selectedCategory,
                    categoryCounts = uiState.categoryCounts,
                    onCategorySelected = { viewModel.onCategorySelected(it) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.searchAndUpdatePrices() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_update_prices")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar Preços",
                        modifier = if (uiState.isUpdating) Modifier.rotate(rotationAngle) else Modifier
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isUpdating) "Pesquisando…" else "Atualizar Preços",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. Hero Culinary Banner tailored for Category
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.pesto_hero_banner),
                        contentDescription = uiState.selectedCategory.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x99000000),
                                        Color(0xEE134E1E)
                                    )
                                )
                            )
                    )

                    // Small Pagueti watermark badge in corner of banner
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.95f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_pagueti),
                            contentDescription = "Pagueti",
                            modifier = Modifier
                                .height(20.dp)
                                .widthIn(max = 80.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.selectedCategory.emoji,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.selectedCategory.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = uiState.selectedCategory.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // 3. Primary Prominent Action Card / Button
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("action_update_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Atualização Automática: ${uiState.selectedCategory.shortName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (uiState.isUpdating) {
                                        uiState.updateStatusMessage ?: "Varrendo redes de supermercados…"
                                    } else {
                                        "Busca cotações e ofertas ativas para ${uiState.selectedCategory.shortName.lowercase()} nas redes brasileiras"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = { viewModel.searchAndUpdatePrices() },
                                enabled = !uiState.isUpdating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("btn_trigger_price_search")
                            ) {
                                if (uiState.isUpdating) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isUpdating) "Buscando…" else "Atualizar",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Animated loading bar during search
                        AnimatedVisibility(visible = uiState.isUpdating) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                                Text(
                                    text = uiState.updateStatusMessage ?: "Atualizando banco de dados…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Quick Comparison Statistics Header
            item {
                PestoStatsHeader(
                    lowestPrice = uiState.lowestPriceProduct,
                    bestValue = uiState.bestValueProduct,
                    averagePrice = uiState.averagePrice,
                    totalCount = uiState.products.size
                )
            }

            // 5. Search and Filter Bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Pesquisar em ${uiState.selectedCategory.shortName} (marca ou nome)…") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar busca")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_text_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Supermarket Chips Filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = uiState.selectedSupermarket == null,
                            onClick = { viewModel.onSupermarketSelected(null) },
                            label = { Text("Todos os Mercados") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_market_all")
                        )

                        uiState.availableSupermarkets.forEach { market ->
                            FilterChip(
                                selected = uiState.selectedSupermarket == market,
                                onClick = {
                                    viewModel.onSupermarketSelected(if (uiState.selectedSupermarket == market) null else market)
                                },
                                label = { Text(market) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("chip_market_${market.lowercase().replace(" ", "_")}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sort and View Mode Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sort Button with dropdown
                        Box {
                            ElevatedButton(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_sort_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ordenar: ${uiState.sortOption.label}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                fontWeight = if (uiState.sortOption == option) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.onSortOptionChanged(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // View mode toggle (Table vs Cards)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(2.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.setViewMode(ViewMode.TABLE) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (uiState.viewMode == ViewMode.TABLE) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .testTag("toggle_view_table")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = "Visualização em Tabela",
                                    tint = if (uiState.viewMode == ViewMode.TABLE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.setViewMode(ViewMode.CARDS) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (uiState.viewMode == ViewMode.CARDS) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .testTag("toggle_view_cards")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewAgenda,
                                    contentDescription = "Visualização em Cartões",
                                    tint = if (uiState.viewMode == ViewMode.CARDS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 6. Section Header with count
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tabela de ${uiState.selectedCategory.title} (${uiState.products.size} itens)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Deslize para os lados ⇄",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // 7. Content (Table or Cards)
            if (uiState.products.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = uiState.selectedCategory.emoji, fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Nenhum produto encontrado em ${uiState.selectedCategory.shortName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Tente alterar os termos da busca ou selecionar outro supermercado.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else if (uiState.viewMode == ViewMode.TABLE) {
                item {
                    PestoTable(
                        products = uiState.products,
                        bestValueProductId = uiState.bestValueProduct?.id,
                        onProductClick = { productForDetail = it },
                        onEditProduct = { productToEdit = it }
                    )
                }
            } else {
                items(uiState.products, key = { it.id }) { product ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        PestoCard(
                            product = product,
                            isBestValue = product.id == uiState.bestValueProduct?.id,
                            onClick = { productForDetail = product },
                            onEdit = { productToEdit = product }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Update Summary
    uiState.lastSummary?.let { summary ->
        UpdateSummaryDialog(
            summary = summary,
            onDismiss = { viewModel.clearSummary() }
        )
    }

    // Dialog: Add / Edit Product
    if (isAddingProduct) {
        AddEditProductDialog(
            initialProduct = null,
            defaultCategory = uiState.selectedCategory,
            onDismiss = { isAddingProduct = false },
            onSave = { newProd ->
                viewModel.addProduct(newProd)
                isAddingProduct = false
            }
        )
    }

    productToEdit?.let { product ->
        AddEditProductDialog(
            initialProduct = product,
            defaultCategory = uiState.selectedCategory,
            onDismiss = { productToEdit = null },
            onSave = { updated ->
                viewModel.updateProduct(updated)
                productToEdit = null
            },
            onDelete = { toDelete ->
                viewModel.deleteProduct(toDelete)
                productToEdit = null
            }
        )
    }

    // Dialog: Product Details
    productForDetail?.let { product ->
        ProductDetailDialog(
            product = product,
            isBestValue = product.id == uiState.bestValueProduct?.id,
            onDismiss = { productForDetail = null },
            onEdit = {
                val toEdit = product
                productForDetail = null
                productToEdit = toEdit
            }
        )
    }
}
