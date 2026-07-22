package kz.nurkanat.nurordertrack.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kz.nurkanat.nurordertrack.R
import kz.nurkanat.nurordertrack.data.model.*
import kz.nurkanat.nurordertrack.di.AppContainer
import kz.nurkanat.nurordertrack.navigation.Screen
import kz.nurkanat.nurordertrack.ui.theme.*
import kz.nurkanat.nurordertrack.utils.FcmHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    navController: NavHostController,
    userRole: UserRole
) {
    val orderRepo = AppContainer.orderRepository
    val userRepo = AppContainer.userRepository
    val context = LocalContext.current
    val currentUser by userRepo.getCurrentUserFlow()
        .collectAsStateWithLifecycle(initialValue = null)
    val logs by orderRepo.getOrderLogsFlow(orderId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var order by remember { mutableStateOf<Order?>(null) }
    var items by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusSheet by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    LaunchedEffect(orderId) {
        orderRepo.getAllOrdersFlow().collect { orders ->
            order = orders.find { it.id == orderId }
            isLoading = false
        }
    }

    LaunchedEffect(orderId) {
        items = orderRepo.getOrderItems(orderId)
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentOrder = order ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.order_not_found))
        }
        return
    }

    val statusColor = when (currentOrder.status) {
        OrderStatus.NEW -> StatusNew
        OrderStatus.IN_PROGRESS -> StatusInProgress
        OrderStatus.DONE -> StatusDone
        OrderStatus.CLOSED -> StatusClosed
        OrderStatus.CANCELLED -> StatusCancelled
    }

    val availableStatuses = when (userRole) {
        UserRole.EXECUTOR -> when (currentOrder.status) {
            OrderStatus.NEW -> listOf(OrderStatus.IN_PROGRESS)
            OrderStatus.IN_PROGRESS -> listOf(OrderStatus.DONE)
            else -> emptyList()
        }
        UserRole.MANAGER, UserRole.ADMIN -> when (currentOrder.status) {
            OrderStatus.NEW -> listOf(OrderStatus.IN_PROGRESS, OrderStatus.CANCELLED)
            OrderStatus.IN_PROGRESS -> listOf(OrderStatus.DONE, OrderStatus.CANCELLED)
            OrderStatus.DONE -> listOf(OrderStatus.CLOSED, OrderStatus.CANCELLED)
            else -> emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentOrder.orderNumber > 0)
                            stringResource(R.string.order_number_title, currentOrder.orderNumber)
                        else
                            stringResource(R.string.order_number_title, orderId.takeLast(6).uppercase()),
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
                actions = {
                    if (userRole == UserRole.ADMIN || userRole == UserRole.MANAGER) {
                        IconButton(onClick = {
                            navController.navigate(Screen.EditOrder.createRoute(orderId))
                        }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.edit),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    if (userRole == UserRole.ADMIN) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            if (availableStatuses.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = { showStatusSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(52.dp)
                    ) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.change_status), fontSize = 16.sp)
                    }
                }
            }
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
            // Статус
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.status_label_short),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                currentOrder.status.displayName(context), // ✅ исправлено
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = statusColor
                            )
                        }
                        Text(
                            "%.0f ₸".format(currentOrder.totalAmount),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Информация о клиенте
            item {
                SectionCard(title = stringResource(R.string.client)) {
                    InfoRow(
                        Icons.Filled.Person,
                        stringResource(R.string.label_name),
                        currentOrder.clientName.ifEmpty { "—" }
                    )
                    if (currentOrder.clientPhone.isNotEmpty()) {
                        InfoRow(
                            Icons.Filled.Phone,
                            stringResource(R.string.phone),
                            currentOrder.clientPhone
                        )
                    }
                }
            }

            // Исполнитель и даты
            item {
                SectionCard(title = stringResource(R.string.details)) {
                    if (currentOrder.assignedToName.isNotEmpty()) {
                        InfoRow(
                            Icons.Filled.Engineering,
                            stringResource(R.string.executor),
                            currentOrder.assignedToName
                        )
                    }
                    InfoRow(
                        Icons.Filled.CalendarToday,
                        stringResource(R.string.created_at),
                        dateFormat.format(currentOrder.createdAt.toDate())
                    )
                    InfoRow(
                        Icons.Filled.Update,
                        stringResource(R.string.updated_at),
                        dateFormat.format(currentOrder.updatedAt.toDate())
                    )
                    if (currentOrder.comment.isNotEmpty()) {
                        InfoRow(
                            Icons.Filled.Comment,
                            stringResource(R.string.comment),
                            currentOrder.comment
                        )
                    }
                }
            }

            // Позиции заказа
            item {
                Text(
                    stringResource(R.string.order_composition),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (items.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_positions),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                items(items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.productName,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "%.0f ₸ × %.1f".format(item.price, item.quantity),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                "%.0f ₸".format(item.total),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // История изменений
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.change_history),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (logs.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.history_empty),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                items(logs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    log.changedByName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    dateFormat.format(log.timestamp.toDate()),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            // ✅ Переводим action
                            val actionDisplay = when (log.action) {
                                "status_changed" -> stringResource(R.string.log_status_changed)
                                "order_edited"   -> stringResource(R.string.log_order_edited)
                                else -> log.action
                            }

                            // ✅ Переводим oldValue/newValue если это статус
                            val oldDisplay = runCatching {
                                OrderStatus.fromString(log.oldValue).displayName(context)
                            }.getOrDefault(log.oldValue)

                            val newDisplay = runCatching {
                                OrderStatus.fromString(log.newValue).displayName(context)
                            }.getOrDefault(log.newValue)

                            Text(
                                "$actionDisplay: $oldDisplay → $newDisplay",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Диалог удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.delete_order)) },
            text = { Text(stringResource(R.string.delete_order_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        kotlinx.coroutines.MainScope().launch {
                            orderRepo.deleteOrder(orderId)
                            FcmHelper.showLocalNotification(
                                context = context,
                                title = context.getString(R.string.notif_order_deleted),
                                body = context.getString(
                                    R.string.notif_order_deleted_body,
                                    currentOrder.clientName
                                )
                            )
                            navController.popBackStack()
                        }
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Смена статуса Bottom Sheet
    if (showStatusSheet) {
        ModalBottomSheet(onDismissRequest = { showStatusSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    stringResource(R.string.change_status),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                availableStatuses.forEach { status ->
                    val color = when (status) {
                        OrderStatus.NEW -> StatusNew
                        OrderStatus.IN_PROGRESS -> StatusInProgress
                        OrderStatus.DONE -> StatusDone
                        OrderStatus.CLOSED -> StatusClosed
                        OrderStatus.CANCELLED -> StatusCancelled
                    }
                    Button(
                        onClick = {
                            kotlinx.coroutines.MainScope().launch {
                                orderRepo.updateOrderStatus(
                                    orderId = orderId,
                                    newStatus = status,
                                    changedByName = currentUser?.name ?: "",
                                    oldStatus = currentOrder.status,
                                    changedById = currentUser?.id ?: ""
                                )
                                FcmHelper.showLocalNotification(
                                    context = context,
                                    title = context.getString(R.string.notif_status_changed),
                                    body = context.getString(
                                        R.string.notif_status_changed_body,
                                        currentOrder.clientName,
                                        currentOrder.status.displayName(context), // ✅ исправлено
                                        status.displayName(context)               // ✅ исправлено
                                    )
                                )
                            }
                            showStatusSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Text(
                            status.displayName(context), // ✅ исправлено
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "$label: ",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}