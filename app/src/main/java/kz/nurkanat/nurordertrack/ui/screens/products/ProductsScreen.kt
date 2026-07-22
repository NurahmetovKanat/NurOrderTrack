package kz.nurkanat.nurordertrack.ui.screens.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kz.nurkanat.nurordertrack.R
import kz.nurkanat.nurordertrack.data.model.Product
import kz.nurkanat.nurordertrack.data.model.ProductType
import kz.nurkanat.nurordertrack.di.AppContainer
import kz.nurkanat.nurordertrack.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(navController: NavHostController) {
    val productRepo = AppContainer.productRepository
    val products by productRepo.getAllProductsFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var showSheet by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    val filtered = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else products.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    // ── разбиваем по типу ──
    val goods    = filtered.filter { it.type == ProductType.PRODUCT }
    val services = filtered.filter { it.type == ProductType.SERVICE }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.products),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingProduct = null; showSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_products)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Inventory2,
                    title = stringResource(R.string.no_products),
                    subtitle = stringResource(R.string.no_products_hint)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    // ── секция Товары ──
                    if (goods.isNotEmpty()) {
                        item {
                            ProductSectionHeader(
                                label = stringResource(R.string.section_products),
                                count = goods.size,
                                icon = Icons.Filled.Inventory2,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(goods, key = { it.id }) { product ->
                            ProductItem(
                                product = product,
                                onEdit = { editingProduct = product; showSheet = true },
                                onArchive = {
                                    scope.launch {
                                        productRepo.archiveProduct(product.id, !product.isArchived)
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // ── секция Услуги ──
                    if (services.isNotEmpty()) {
                        item {
                            ProductSectionHeader(
                                label = stringResource(R.string.section_services),
                                count = services.size,
                                icon = Icons.Filled.Build,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        items(services, key = { it.id }) { product ->
                            ProductItem(
                                product = product,
                                onEdit = { editingProduct = product; showSheet = true },
                                onArchive = {
                                    scope.launch {
                                        productRepo.archiveProduct(product.id, !product.isArchived)
                                    }
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showSheet) {
        ProductSheet(
            product = editingProduct,
            onDismiss = { showSheet = false },
            onSave = { product ->
                scope.launch {
                    productRepo.saveProduct(product)
                }
                showSheet = false
            }
        )
    }
}

// ── Заголовок секции ──────────────────────────────────────────────────────────
@Composable
fun ProductSectionHeader(
    label: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = color.copy(alpha = 0.12f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$label ($count)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = color.copy(alpha = 0.25f),
            thickness = 1.dp
        )
    }
}

// ── Карточка товара/услуги ────────────────────────────────────────────────────
@Composable
fun ProductItem(
    product: Product,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    val typeColor = when (product.type) {
        ProductType.PRODUCT -> MaterialTheme.colorScheme.primary
        ProductType.SERVICE -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (product.isArchived)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        product.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    if (product.isArchived) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                stringResource(R.string.archive),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // ── для товара показываем цену + единицу, для услуги — только цену ──
                Text(
                    if (product.type == ProductType.PRODUCT)
                        "%.0f ₸ / ${product.unit}".format(product.price)
                    else
                        "%.0f ₸".format(product.price),
                    fontSize = 13.sp,
                    color = typeColor
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                }
                IconButton(onClick = onArchive) {
                    Icon(
                        if (product.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                        contentDescription = stringResource(R.string.archive),
                        tint = if (product.isArchived)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ── Bottom Sheet добавления/редактирования ────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSheet(
    product: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val units = listOf(
        stringResource(R.string.unit_pcs),
        stringResource(R.string.unit_kg),
        stringResource(R.string.unit_hour),
        stringResource(R.string.unit_m2),
        stringResource(R.string.unit_m),
        stringResource(R.string.unit_l)
    )

    var name             by remember { mutableStateOf(product?.name ?: "") }
    var price            by remember { mutableStateOf(product?.price?.toString() ?: "") }
    val defaultUnit = stringResource(R.string.unit_pcs)
    var unit             by remember { mutableStateOf(product?.unit?.ifEmpty { null } ?: defaultUnit) }
    var selectedType     by remember { mutableStateOf(product?.type ?: ProductType.PRODUCT) }
    var showUnitDropdown by remember { mutableStateOf(false) }
    var nameError        by remember { mutableStateOf(false) }
    var priceError       by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (product == null) stringResource(R.string.new_product_or_service)
                else stringResource(R.string.edit),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            // ── переключатель Товар / Услуга ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypeToggleButton(
                    label = stringResource(R.string.type_product),
                    icon = Icons.Filled.Inventory2,
                    selected = selectedType == ProductType.PRODUCT,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { selectedType = ProductType.PRODUCT },
                    modifier = Modifier.weight(1f)
                )
                TypeToggleButton(
                    label = stringResource(R.string.type_service),
                    icon = Icons.Filled.Build,
                    selected = selectedType == ProductType.SERVICE,
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { selectedType = ProductType.SERVICE },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = {
                    Text(
                        if (selectedType == ProductType.PRODUCT) stringResource(R.string.product_name_label)
                        else stringResource(R.string.service_name_label)
                    )
                },
                isError = nameError,
                supportingText = { if (nameError) Text(stringResource(R.string.required_field)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ── цена + единица (единица только для товара) ──
            if (selectedType == ProductType.PRODUCT) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it; priceError = false },
                        label = { Text(stringResource(R.string.price)) },
                        isError = priceError,
                        supportingText = {
                            if (priceError) Text(stringResource(R.string.error_invalid_number))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    ExposedDropdownMenuBox(
                        expanded = showUnitDropdown,
                        onExpandedChange = { showUnitDropdown = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.unit)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showUnitDropdown)
                            },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showUnitDropdown,
                            onDismissRequest = { showUnitDropdown = false }
                        ) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = { unit = u; showUnitDropdown = false }
                                )
                            }
                        }
                    }
                }
            } else {
                // ── для услуги — только цена, на всю ширину ──
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it; priceError = false },
                    label = { Text(stringResource(R.string.price)) },
                    isError = priceError,
                    supportingText = {
                        if (priceError) Text(stringResource(R.string.error_invalid_number))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    nameError  = name.isBlank()
                    priceError = price.toDoubleOrNull() == null
                    if (!nameError && !priceError) {
                        onSave(
                            Product(
                                id         = product?.id ?: "",
                                name       = name.trim(),
                                price      = price.toDouble(),
                                unit       = if (selectedType == ProductType.PRODUCT) unit else "",
                                type       = selectedType,
                                isArchived = product?.isArchived ?: false
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.save), fontSize = 16.sp)
            }
        }
    }
}

// ── Кнопка переключения типа ──────────────────────────────────────────────────
@Composable
fun TypeToggleButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) color.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected)
            androidx.compose.foundation.BorderStroke(1.5.dp, color)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) color
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) color
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}