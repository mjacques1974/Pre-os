package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PestoProduct
import com.example.ui.theme.BestValueGold
import com.example.ui.theme.DiscountGreen
import com.example.ui.theme.IncreaseRed
import java.util.Locale

@Composable
fun PestoTable(
    products: List<PestoProduct>,
    bestValueProductId: Long?,
    onProductClick: (PestoProduct) -> Unit,
    onEditProduct: (PestoProduct) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()

    val unitLabel = products.firstOrNull()?.unitLabel ?: "g"

    // Fixed column widths for consistent table alignment
    val brandColWidth = 140.dp
    val productColWidth = 175.dp
    val marketColWidth = 125.dp
    val weightColWidth = 80.dp
    val priceColWidth = 110.dp
    val unitColWidth = 95.dp
    val actionsColWidth = 50.dp

    val totalTableWidth = brandColWidth + productColWidth + marketColWidth +
            weightColWidth + priceColWidth + unitColWidth + actionsColWidth

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("pesto_table_surface"),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            Column(modifier = Modifier.width(totalTableWidth)) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableHeaderCell("Marca", brandColWidth)
                    TableHeaderCell("Produto", productColWidth)
                    TableHeaderCell("Supermercado", marketColWidth)
                    TableHeaderCell(if (unitLabel == "ml") "Volume" else "Peso", weightColWidth, align = TextAlign.Center)
                    TableHeaderCell("Preço", priceColWidth, align = TextAlign.End)
                    TableHeaderCell("R$/100$unitLabel", unitColWidth, align = TextAlign.End)
                    TableHeaderCell("", actionsColWidth, align = TextAlign.Center)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                // Table Data Rows
                products.forEachIndexed { index, product ->
                    val isEven = index % 2 == 0
                    val isBestValue = product.id == bestValueProductId

                    val rowBg = when {
                        product.isPromo -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        isEven -> MaterialTheme.colorScheme.surface
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .clickable { onProductClick(product) }
                            .padding(vertical = 10.dp, horizontal = 8.dp)
                            .testTag("table_row_${product.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand Column with origin
                        Column(
                            modifier = Modifier.width(brandColWidth),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = product.brand,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = product.origin,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Product Name Column
                        Column(
                            modifier = Modifier
                                .width(productColWidth)
                                .padding(end = 6.dp)
                        ) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (product.isPromo) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(DiscountGreen)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "OFERTA",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Supermarket Column
                        Box(
                            modifier = Modifier
                                .width(marketColWidth)
                                .padding(end = 6.dp)
                        ) {
                            SupermarketBadge(supermarket = product.supermarket)
                        }

                        // Weight/Volume Column
                        Text(
                            text = product.formattedWeight(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(weightColWidth),
                            textAlign = TextAlign.Center
                        )

                        // Price Column
                        Column(
                            modifier = Modifier
                                .width(priceColWidth)
                                .padding(end = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = product.formattedPrice(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (product.isPromo) DiscountGreen else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End
                            )

                            // Previous Price & Diff Indicator
                            if (product.previousPriceBrl != null && product.previousPriceBrl != product.priceBrl) {
                                val diff = product.priceDiff ?: 0.0
                                val percent = product.priceDiffPercent ?: 0.0
                                val isDrop = diff < 0

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = product.formattedPreviousPrice() ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp,
                                            textDecoration = TextDecoration.LineThrough
                                        ),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Icon(
                                        imageVector = if (isDrop) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (isDrop) DiscountGreen else IncreaseRed,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%+.1f%%", percent),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDrop) DiscountGreen else IncreaseRed
                                    )
                                }
                            }
                        }

                        // Price per 100g/ml Column
                        Column(
                            modifier = Modifier
                                .width(unitColWidth)
                                .padding(end = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = product.formattedPricePer100g(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isBestValue) FontWeight.Bold else FontWeight.Normal,
                                color = if (isBestValue) BestValueGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End
                            )
                            if (isBestValue) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 1.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Melhor Custo",
                                        tint = BestValueGold,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "Top Custo",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = BestValueGold
                                    )
                                }
                            }
                        }

                        // Actions Column
                        Box(
                            modifier = Modifier.width(actionsColWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { onEditProduct(product) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("edit_product_${product.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (index < products.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.width(width),
        textAlign = align
    )
}

@Composable
fun SupermarketBadge(
    supermarket: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (supermarket.lowercase()) {
        "pão de açúcar" -> Color(0xFFE8F5E9) to Color(0xFF1B5E20)
        "carrefour" -> Color(0xFFE3F2FD) to Color(0xFF0D47A1)
        "st. marche" -> Color(0xFFFFF3E0) to Color(0xFFBF360C)
        "mambo" -> Color(0xFFFCE4EC) to Color(0xFF880E4F)
        "sonda supermercados", "sonda" -> Color(0xFFEDE7F6) to Color(0xFF4A148C)
        "oba hortifruti", "oba" -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = supermarket,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
