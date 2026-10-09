package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.PestoProduct
import com.example.data.ProductCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    initialProduct: PestoProduct? = null,
    defaultCategory: ProductCategory = ProductCategory.PESTO,
    onDismiss: () -> Unit,
    onSave: (PestoProduct) -> Unit,
    onDelete: ((PestoProduct) -> Unit)? = null
) {
    val isEdit = initialProduct != null

    var selectedCat by remember {
        mutableStateOf(
            if (initialProduct != null) ProductCategory.fromId(initialProduct.category) else defaultCategory
        )
    }
    var catDropdownExpanded by remember { mutableStateOf(false) }

    var brand by remember { mutableStateOf(initialProduct?.brand ?: "") }
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var weightText by remember { mutableStateOf(initialProduct?.weightGrams?.toString() ?: "190") }
    var supermarket by remember { mutableStateOf(initialProduct?.supermarket ?: "Pão de Açúcar") }
    var priceText by remember {
        mutableStateOf(initialProduct?.priceBrl?.let { String.format("%.2f", it).replace('.', ',') } ?: "")
    }
    var origin by remember { mutableStateOf(initialProduct?.origin ?: "Itália 🇮🇹") }
    var notes by remember { mutableStateOf(initialProduct?.ingredientsNote ?: "") }
    var isPromo by remember { mutableStateOf(initialProduct?.isPromo ?: false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Editar Produto" else "Novo Produto no Comparador",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = catDropdownExpanded,
                    onExpandedChange = { catDropdownExpanded = !catDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = "${selectedCat.emoji} ${selectedCat.title}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria do Produto") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = catDropdownExpanded,
                        onDismissRequest = { catDropdownExpanded = false }
                    ) {
                        ProductCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.emoji} ${cat.title}") },
                                onClick = {
                                    selectedCat = cat
                                    catDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Marca (ex: Barilla, Hemmer, Heinz)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_product_brand")
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Produto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_product_name")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text(if (selectedCat.unitLabel == "ml") "Volume (ml)" else "Peso (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_product_weight")
                    )

                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Preço R$ (ex: 29,90)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_product_price")
                    )
                }

                OutlinedTextField(
                    value = supermarket,
                    onValueChange = { supermarket = it },
                    label = { Text("Supermercado (ex: Carrefour, Pão de Açúcar)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_product_supermarket")
                )

                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("Origem (ex: Brasil 🇧🇷, Itália 🇮🇹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ingredientes / Observações") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Checkbox(
                        checked = isPromo,
                        onCheckedChange = { isPromo = it },
                        modifier = Modifier.testTag("checkbox_promo")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Produto em Oferta / Promoção",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPrice = priceText.replace(',', '.').toDoubleOrNull()
                    val parsedWeight = weightText.toIntOrNull()

                    if (brand.isBlank() || name.isBlank()) {
                        errorMessage = "Preencha a marca e o nome do produto."
                        return@Button
                    }
                    if (parsedPrice == null || parsedPrice <= 0) {
                        errorMessage = "Digite um preço válido (ex: 29,90)."
                        return@Button
                    }
                    if (parsedWeight == null || parsedWeight <= 0) {
                        errorMessage = "Digite um peso/volume válido."
                        return@Button
                    }

                    val updated = (initialProduct ?: PestoProduct(
                        category = selectedCat.id,
                        brand = brand.trim(),
                        name = name.trim(),
                        weightGrams = parsedWeight,
                        supermarket = supermarket.trim(),
                        priceBrl = parsedPrice,
                        origin = origin.trim(),
                        ingredientsNote = notes.trim(),
                        isPromo = isPromo
                    )).copy(
                        category = selectedCat.id,
                        brand = brand.trim(),
                        name = name.trim(),
                        weightGrams = parsedWeight,
                        supermarket = supermarket.trim(),
                        priceBrl = parsedPrice,
                        origin = origin.trim(),
                        ingredientsNote = notes.trim(),
                        isPromo = isPromo
                    )

                    onSave(updated)
                },
                modifier = Modifier.testTag("btn_save_product")
            ) {
                Text(if (isEdit) "Salvar" else "Adicionar")
            }
        },
        dismissButton = {
            Row {
                if (isEdit && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(initialProduct) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("btn_delete_product")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
