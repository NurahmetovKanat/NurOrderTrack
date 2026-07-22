package kz.nurkanat.nurordertrack.ui.screens.dashboard

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kz.nurkanat.nurordertrack.R
import kz.nurkanat.nurordertrack.data.model.Order
import kz.nurkanat.nurordertrack.data.model.OrderStatus
import kz.nurkanat.nurordertrack.data.model.UserRole
import kz.nurkanat.nurordertrack.di.AppContainer
import kz.nurkanat.nurordertrack.navigation.Screen
import kz.nurkanat.nurordertrack.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kz.nurkanat.nurordertrack.data.model.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController) {
    val orderRepo = AppContainer.orderRepository
    val userRepo = AppContainer.userRepository

    val currentUser by userRepo.getCurrentUserFlow()
        .collectAsStateWithLifecycle(initialValue = null)

    val allOrders by orderRepo.getAllOrdersFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val myOrders = remember(allOrders, currentUser) {
        when (currentUser?.role) {
            UserRole.EXECUTOR -> allOrders.filter { it.assignedTo == currentUser?.id }
            else -> allOrders
        }
    }

    val recentOrders = remember(myOrders) {
        myOrders.sortedByDescending { it.createdAt }.take(5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.welcome),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = currentUser?.name ?: "NurOrderTrack",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    if (currentUser?.role == UserRole.ADMIN) {
                        IconButton(onClick = {
                            navController.navigate(Screen.Users.route)
                        }) {
                            Icon(
                                Icons.Filled.ManageAccounts,
                                contentDescription = stringResource(R.string.users),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(onClick = {
                            navController.navigate(Screen.Products.route)
                        }) {
                            Icon(
                                Icons.Filled.Inventory,
                                contentDescription = stringResource(R.string.products),
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
            item {
                Text(
                    text = stringResource(R.string.summary),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.status_new),
                        count = myOrders.count { it.status == OrderStatus.NEW },
                        color = StatusNew,
                        icon = Icons.Filled.FiberNew
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.status_in_progress),
                        count = myOrders.count { it.status == OrderStatus.IN_PROGRESS },
                        color = StatusInProgress,
                        icon = Icons.Filled.Build
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.status_done),
                        count = myOrders.count { it.status == OrderStatus.DONE },
                        color = StatusDone,
                        icon = Icons.Filled.CheckCircle
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.status_closed),
                        count = myOrders.count { it.status == OrderStatus.CLOSED },
                        color = StatusClosed,
                        icon = Icons.Filled.Lock
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recent_orders),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { navController.navigate(Screen.Orders.route) }) {
                        Text(stringResource(R.string.all_orders))
                    }
                }
            }

            if (recentOrders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_orders),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                items(recentOrders) { order ->
                    OrderCard(
                        order = order,
                        onClick = {
                            navController.navigate(Screen.OrderDetail.createRoute(order.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    color: Color,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = count.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit
) {
    val statusColor = when (order.status) {
        OrderStatus.NEW -> StatusNew
        OrderStatus.IN_PROGRESS -> StatusInProgress
        OrderStatus.DONE -> StatusDone
        OrderStatus.CLOSED -> StatusClosed
        OrderStatus.CANCELLED -> StatusCancelled
    }
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (order.orderNumber > 0)
                        "#${order.orderNumber} · ${order.clientName.ifEmpty { "—" }}"
                    else
                        order.clientName.ifEmpty { "—" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(order.createdAt.toDate()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (order.assignedToName.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.executor_prefix, order.assignedToName),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        order.status.displayName(context),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "%.0f ₸".format(order.totalAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}