package kz.nurkanat.nurordertrack.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kz.nurkanat.nurordertrack.R
import kz.nurkanat.nurordertrack.data.model.OrderStatus
import kz.nurkanat.nurordertrack.data.model.UserRole
import kz.nurkanat.nurordertrack.di.AppContainer
import kz.nurkanat.nurordertrack.ui.theme.*
import kz.nurkanat.nurordertrack.utils.ExcelExporter
import java.util.*
import kz.nurkanat.nurordertrack.data.model.displayName

enum class AnalyticsPeriod(val labelRes: Int) {
    TODAY(R.string.today),
    WEEK(R.string.week),
    MONTH(R.string.month),
    ALL(R.string.all_time)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(navController: NavHostController, userRole: UserRole) {
    val orderRepo = AppContainer.orderRepository
    val allOrders by orderRepo.getAllOrdersFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf(AnalyticsPeriod.ALL) }

    val filteredOrders = remember(allOrders, selectedPeriod) {
        val now = Calendar.getInstance()
        allOrders.filter { order ->
            when (selectedPeriod) {
                AnalyticsPeriod.TODAY -> {
                    val cal = Calendar.getInstance()
                    cal.time = order.createdAt.toDate()
                    cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                            cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                }
                AnalyticsPeriod.WEEK -> {
                    val weekAgo = Calendar.getInstance()
                    weekAgo.add(Calendar.DAY_OF_YEAR, -7)
                    order.createdAt.toDate().after(weekAgo.time)
                }
                AnalyticsPeriod.MONTH -> {
                    val monthAgo = Calendar.getInstance()
                    monthAgo.add(Calendar.MONTH, -1)
                    order.createdAt.toDate().after(monthAgo.time)
                }
                AnalyticsPeriod.ALL -> true
            }
        }
    }

    val totalAmount = remember(filteredOrders) {
        filteredOrders.sumOf { it.totalAmount }
    }

    val topClients = remember(filteredOrders) {
        filteredOrders
            .groupBy { it.clientName }
            .map { (name, orders) -> name to orders.sumOf { it.totalAmount } }
            .sortedByDescending { it.second }
            .take(5)
    }

    val statusCounts = remember(filteredOrders) {
        OrderStatus.entries.map { status ->
            status to filteredOrders.count { it.status == status }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.analytics),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            isExporting = true
                            scope.launch {
                                try {
                                    val file = ExcelExporter.exportOrders(
                                        context = context,
                                        orders = filteredOrders,
                                        getItems = { orderId ->
                                            AppContainer.orderRepository.getOrderItems(orderId)
                                        }
                                    )
                                    ExcelExporter.shareFile(context, file)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        enabled = !isExporting && filteredOrders.isNotEmpty()
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Filled.FileDownload,
                                contentDescription = stringResource(R.string.export_excel),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
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
            // Фильтр периода
            item {
                Text(
                    stringResource(R.string.period),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = {
                                Text(
                                    stringResource(period.labelRes),
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Общая статистика
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                .copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.ListAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                filteredOrders.size.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                stringResource(R.string.total_orders),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = StatusDone.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Payments,
                                contentDescription = null,
                                tint = StatusDone,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "%.0f".format(totalAmount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = StatusDone
                            )
                            Text(
                                stringResource(R.string.total_amount),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Круговая диаграмма
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.status_distribution),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        if (filteredOrders.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.no_data),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PieChart(
                                    data = statusCounts,
                                    modifier = Modifier.size(160.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    statusCounts.forEach { (status, count) ->
                                        if (count > 0) {
                                            val color = when (status) {
                                                OrderStatus.NEW -> StatusNew
                                                OrderStatus.IN_PROGRESS -> StatusInProgress
                                                OrderStatus.DONE -> StatusDone
                                                OrderStatus.CLOSED -> StatusClosed
                                                OrderStatus.CANCELLED -> StatusCancelled
                                            }
                                            LegendItem(
                                                    color = color,
                                            label = status.displayName(context),
                                                count = count,
                                                total = filteredOrders.size
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Детальная статистика
            item {
                Text(
                    stringResource(R.string.detailed_stats),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            items(statusCounts) { (status, count) ->
                val color = when (status) {
                    OrderStatus.NEW -> StatusNew
                    OrderStatus.IN_PROGRESS -> StatusInProgress
                    OrderStatus.DONE -> StatusDone
                    OrderStatus.CLOSED -> StatusClosed
                    OrderStatus.CANCELLED -> StatusCancelled
                }
                val amount = filteredOrders
                    .filter { it.status == status }
                    .sumOf { it.totalAmount }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = color.copy(alpha = 0.07f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = color.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        count.toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                status.displayName(context),
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            "%.0f ₸".format(amount),
                            fontWeight = FontWeight.SemiBold,
                            color = color,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Топ клиентов
            if (topClients.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.top_clients),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                items(topClients.withIndex().toList()) { (index, pair) ->
                    val (clientName, amount) = pair
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = when (index) {
                                        0 -> Color(0xFFFFD700).copy(alpha = 0.2f)
                                        1 -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
                                        2 -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "#${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = when (index) {
                                                0 -> Color(0xFFFFD700)
                                                1 -> Color(0xFF909090)
                                                2 -> Color(0xFFCD7F32)
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    clientName.ifEmpty { stringResource(R.string.no_name) },
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                "%.0f ₸".format(amount),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun PieChart(
    data: List<Pair<OrderStatus, Int>>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(StatusNew, StatusInProgress, StatusDone, StatusClosed, StatusCancelled)
    val total = data.sumOf { it.second }.toFloat()
    if (total == 0f) return

    Canvas(modifier = modifier) {
        var startAngle = -90f
        data.forEachIndexed { index, (_, count) ->
            if (count > 0) {
                val sweep = (count / total) * 360f
                drawArc(
                    color = colors[index],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(size.width * 0.05f, size.height * 0.05f),
                    size = Size(size.width * 0.9f, size.height * 0.9f)
                )
                startAngle += sweep
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, count: Int, total: Int) {
    val percent = if (total > 0) (count * 100f / total) else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = color,
            modifier = Modifier.size(12.dp)
        ) {}
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "$label: $count (%.0f%%)".format(percent),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}