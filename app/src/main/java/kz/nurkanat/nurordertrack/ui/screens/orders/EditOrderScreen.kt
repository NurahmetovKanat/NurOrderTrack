package kz.nurkanat.nurordertrack.ui.screens.orders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import kz.nurkanat.nurordertrack.R
import kz.nurkanat.nurordertrack.data.model.*
import kz.nurkanat.nurordertrack.di.AppContainer
import kz.nurkanat.nurordertrack.ui.components.ExitConfirmDialog
import kz.nurkanat.nurordertrack.utils.FcmHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrderScreen(orderId: String, navController: NavHostController) {
    val orderRepo = AppContainer.orderRepository
    val clientRepo = AppContainer.clientRepository
    val productRepo = AppContainer.productRepository
    val userRepo = AppContainer.userRepository
    val context = LocalContext.current

    val clients by clientRepo.getAllClientsFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val products by productRepo.getAllProductsFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val users by userRepo.getAllUsersFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val currentUser by userRepo.getCurrentUserFlow()
        .collectAsStateWithLifecycle(initialValue = null)

    val activeProducts = remember(products) { products.filter { !it.isArchived } }
    val executors = remember(users) {
        users.filter { it.role == UserRole.EXECUTOR && it.isActive }
    }

    var isLoaded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showExitDialog by remember { mutableStateOf(false) }

    var clientQuery by remember { mutableStateOf("") }
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var manualClientPhone by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var selectedExecutor by remember { mutableStateOf<User?>(null) }
    var orderItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var originalOrder by remember { mutableStateOf<Order?>(null) }

    var showClientDropdown by remember { mutableStateOf(false) }
    var showProductSheet by remember { mutableStateOf(false) }
    var showExecutorDropdown by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val totalAmount = remember(orderItems) { orderItems.sumOf { it.total } }

    val filteredClients = remember(clients, clientQuery) {
        if (clientQuery.length >= 1)
            clients.filter {
                it.name.contains(clientQuery, ignoreCase = true) ||
                        it.phone.contains(clientQuery)
            }
        else emptyList()
    }

    LaunchedEffect(orderId) {
        orderRepo.getAllOrdersFlow().collect { orders ->
            val order = orders.find { it.id == orderId }
            if (order != null && !isLoaded) {
                originalOrder = order
                clientQuery = order.clientName
                manualClientPhone = order.clientPhone
                comment = order.comment
                selectedClient = clients.find { it.id == order.clientId }
                val loadedItems = orderRepo.getOrderItems(orderId)
                orderItems = loadedItems
                selectedExecutor = users.find { it.id == order.assignedTo }
                isLoaded = true
            }
        }
    }

    LaunchedEffect(users, originalOrder) {
        if (originalOrder != null && users.isNotEmpty()) {
            selectedExecutor = users.find { it.id == originalOrder!!.assignedTo }
        }
    }

    BackHandler {
        showExitDialog = true
    }

    if (!isLoaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.edit_order),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Клиент
            item {
                Text(
                    stringResource(R.string.client),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = showClientDropdown && filteredClients.isNotEmpty(),
                    onExpandedChange = {}
                ) {
                    OutlinedTextField(
                        value = if (selectedClient != null) selectedClient!!.name else clientQuery,
                        onValueChange = {
                            clientQuery = it
                            selectedClient = null
                            showClientDropdown = true
                        },
                        label = { Text(stringResource(R.string.client_search)) },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        trailingIcon = {
                            if (selectedClient != null) {
                                IconButton(onClick = {
                                    selectedClient = null
                                    clientQuery = ""
                                }) {
                                    Icon(Icons.Filled.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showClientDropdown && filteredClients.isNotEmpty(),
                        onDismissRequest = { showClientDropdown = false }
                    ) {
                        filteredClients.forEach { client ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(client.name, fontWeight = FontWeight.Medium)
                                        if (client.phone.isNotEmpty()) {
                                            Text(
                                                client.phone,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                                    .copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedClient = client
                                    clientQuery = client.name
                                    showClientDropdown = false
                                }
                            )
                        }
                    }
                }
                if (selectedClient == null && clientQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualClientPhone,
                        onValueChange = { manualClientPhone = it },
                        label = { Text(stringResource(R.string.client_phone)) },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Исполнитель
            item {
                Text(
                    stringResource(R.string.executor),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = showExecutorDropdown,
                    onExpandedChange = { showExecutorDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedExecutor?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.select_executor)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Engineering, contentDescription = null)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = showExecutorDropdown
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showExecutorDropdown,
                        onDismissRequest = { showExecutorDropdown = false }
                    ) {
                        executors.forEach { user ->
                            DropdownMenuItem(
                                text = { Text(user.name) },
                                onClick = {
                                    selectedExecutor = user
                                    showExecutorDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Позиции
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.positions),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    TextButton(onClick = { showProductSheet = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.add))
                    }
                }
            }

            if (orderItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.add_positions),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            itemsIndexed(orderItems) { index, item ->
                OrderItemRow(
                    item = item,
                    onQuantityChange = { newQty ->
                        orderItems = orderItems.toMutableList().also {
                            it[index] = item.copy(quantity = newQty)
                        }
                    },
                    onDelete = {
                        orderItems = orderItems.toMutableList().also { it.removeAt(index) }
                    }
                )
            }

            // Итого
            if (orderItems.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                .copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.total),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "%.0f ₸".format(totalAmount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Комментарий
            item {
                Text(
                    stringResource(R.string.comment),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(stringResource(R.string.optional)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (errorMessage.isNotEmpty()) {
                item {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }
            }

            // Кнопка сохранить
            item {
                Button(
                    onClick = {
                        val clientName = selectedClient?.name ?: clientQuery
                        if (clientName.isBlank()) {
                            errorMessage = context.getString(R.string.specify_client)
                            return@Button
                        }
                        if (orderItems.isEmpty()) {
                            errorMessage = context.getString(R.string.add_position)
                            return@Button
                        }
                        isLoading = true
                        errorMessage = ""
                        kotlinx.coroutines.MainScope().launch {
                            val updatedOrder = originalOrder!!.copy(
                                clientId = selectedClient?.id ?: originalOrder!!.clientId,
                                clientName = clientName,
                                clientPhone = selectedClient?.phone ?: manualClientPhone,
                                comment = comment,
                                assignedTo = selectedExecutor?.id ?: "",
                                assignedToName = selectedExecutor?.name ?: "",
                                totalAmount = totalAmount,
                                updatedAt = Timestamp.now()
                            )
                            val result = orderRepo.updateOrder(updatedOrder, orderItems)
                            if (result.isSuccess) {
                                orderRepo.addLog(
                                    orderId = orderId,
                                    action = "order_edited",
                                    changedBy = currentUser?.id ?: "",
                                    changedByName = currentUser?.name ?: "",
                                    oldValue = "—",
                                    newValue = "data_updated"
                                )
                                FcmHelper.showLocalNotification(
                                    context = context,
                                    title = context.getString(R.string.notif_order_updated),
                                    body = context.getString(
                                        R.string.notif_order_updated_body,
                                        updatedOrder.clientName
                                    )
                                )
                                navController.popBackStack()
                            } else {
                                errorMessage = context.getString(R.string.save_error)
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.save_changes), fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Выбор товара
    if (showProductSheet) {
        ModalBottomSheet(onDismissRequest = { showProductSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    stringResource(R.string.select_product),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                activeProducts.forEach { product ->
                    ListItem(
                        headlineContent = { Text(product.name) },
                        supportingContent = {
                            Text("%.0f ₸ / ${product.unit}".format(product.price))
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                val existing = orderItems.indexOfFirst {
                                    it.productId == product.id
                                }
                                orderItems = if (existing >= 0) {
                                    orderItems.toMutableList().also {
                                        it[existing] = it[existing].copy(
                                            quantity = it[existing].quantity + 1
                                        )
                                    }
                                } else {
                                    orderItems + OrderItem(
                                        productId = product.id,
                                        productName = product.name,
                                        quantity = 1.0,
                                        price = product.price
                                    )
                                }
                                showProductSheet = false
                            }) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showExitDialog) {
        ExitConfirmDialog(
            onConfirm = {
                showExitDialog = false
                navController.popBackStack()
            },
            onDismiss = { showExitDialog = false }
        )
    }
}