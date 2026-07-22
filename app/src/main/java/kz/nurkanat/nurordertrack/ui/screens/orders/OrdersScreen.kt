package kz.nurkanat.nurordertrack.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kz.nurkanat.nurordertrack.R
import kz.nurkanat.nurordertrack.data.model.OrderStatus
import kz.nurkanat.nurordertrack.data.model.UserRole
import kz.nurkanat.nurordertrack.di.AppContainer
import kz.nurkanat.nurordertrack.navigation.Screen
import kz.nurkanat.nurordertrack.ui.components.EmptyState
import kz.nurkanat.nurordertrack.ui.screens.dashboard.OrderCard
import java.text.SimpleDateFormat
import java.util.*
import kz.nurkanat.nurordertrack.data.model.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(navController: NavHostController, userRole: UserRole) {
    val orderRepo = AppContainer.orderRepository
    val userRepo = AppContainer.userRepository
    val context = LocalContext.current

    val currentUser by userRepo.getCurrentUserFlow()
        .collectAsStateWithLifecycle(initialValue = null)
    val allOrders by orderRepo.getAllOrdersFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<OrderStatus?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }

    var dateFromMillis by remember { mutableStateOf<Long?>(null) }
    var dateToMillis by remember { mutableStateOf<Long?>(null) }

    val dateRangePickerState = rememberDateRangePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    val filteredOrders = remember(
        allOrders, searchQuery, selectedStatus,
        currentUser, dateFromMillis, dateToMillis
    ) {
        var list = when (currentUser?.role) {
            UserRole.EXECUTOR -> allOrders.filter { it.assignedTo == currentUser?.id }
            else -> allOrders
        }
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.clientName.contains(searchQuery, ignoreCase = true) ||
                        it.clientPhone.contains(searchQuery) ||
                        it.id.contains(searchQuery, ignoreCase = true) ||
                        (it.orderNumber > 0 && it.orderNumber.toString().contains(searchQuery))
            }
        }
        if (selectedStatus != null) {
            list = list.filter { it.status == selectedStatus }
        }
        if (dateFromMillis != null) {
            list = list.filter { it.createdAt.toDate().time >= dateFromMillis!! }
        }
        if (dateToMillis != null) {
            val endOfDay = dateToMillis!! + 24 * 60 * 60 * 1000 - 1
            list = list.filter { it.createdAt.toDate().time <= endOfDay }
        }
        list
    }

    val hasActiveFilters = selectedStatus != null ||
            dateFromMillis != null || dateToMillis != null

    // dateRangeLabel вычисляем через строки — нужен context
    // Используем composable-уровень через stringResource
    val dateFromLabel = stringResource(R.string.date_from_label)
    val dateToLabel = stringResource(R.string.date_to_label)

    val dateRangeLabel = remember(dateFromMillis, dateToMillis) {
        when {
            dateFromMillis != null && dateToMillis != null ->
                "${dateFormat.format(Date(dateFromMillis!!))} — ${dateFormat.format(Date(dateToMillis!!))}"
            dateFromMillis != null ->
                dateFromLabel.format(dateFormat.format(Date(dateFromMillis!!)))
            dateToMillis != null ->
                dateToLabel.format(dateFormat.format(Date(dateToMillis!!)))
            else -> null
        }
    }

    // Быстрые кнопки периода
    val todayLabel = stringResource(R.string.today)
    val weekLabel = stringResource(R.string.week)
    val monthLabel = stringResource(R.string.month)
    val quickPeriods = listOf(todayLabel to 0, weekLabel to 7, monthLabel to 30)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.orders),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    BadgedBox(
                        badge = { if (hasActiveFilters) Badge() }
                    ) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = stringResource(R.string.filter),
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
        floatingActionButton = {
            if (userRole == UserRole.ADMIN || userRole == UserRole.MANAGER) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.CreateOrder.route) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.create_order),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
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
                placeholder = { Text(stringResource(R.string.search_orders_hint)) },
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

            if (hasActiveFilters) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (selectedStatus != null) {
                        AssistChip(
                            onClick = { selectedStatus = null },
                            label = { Text(selectedStatus!!.displayName(context), fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                    if (dateRangeLabel != null) {
                        AssistChip(
                            onClick = {
                                dateFromMillis = null
                                dateToMillis = null
                            },
                            label = { Text(dateRangeLabel, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.found) + " ${filteredOrders.size}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (filteredOrders.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = stringResource(R.string.no_orders_found),
                    subtitle = if (hasActiveFilters || searchQuery.isNotBlank())
                        stringResource(R.string.no_orders_filter_hint)
                    else
                        stringResource(R.string.no_orders_hint)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredOrders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            onClick = {
                                navController.navigate(
                                    Screen.OrderDetail.createRoute(order.id)
                                )
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Фильтр Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.filters),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (hasActiveFilters) {
                        TextButton(onClick = {
                            selectedStatus = null
                            dateFromMillis = null
                            dateToMillis = null
                            showFilterSheet = false
                        }) {
                            Text(stringResource(R.string.reset_all))
                        }
                    }
                }

                Text(
                    stringResource(R.string.filter_by_status),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )

                FilterChipRow(
                    label = stringResource(R.string.all_statuses),
                    selected = selectedStatus == null,
                    onClick = { selectedStatus = null }
                )

                OrderStatus.entries.forEach { status ->
                    FilterChipRow(
                        label = status.displayName(context),
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status }
                    )
                }

                HorizontalDivider()

                Text(
                    stringResource(R.string.period),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPeriods.forEach { (label, days) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                val cal = Calendar.getInstance()
                                dateToMillis = cal.timeInMillis
                                if (days > 0) cal.add(Calendar.DAY_OF_YEAR, -days)
                                dateFromMillis = cal.timeInMillis
                            },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateRangeLabel
                            ?: stringResource(R.string.select_period_calendar),
                        fontSize = 14.sp
                    )
                }

                if (dateRangeLabel != null) {
                    TextButton(
                        onClick = {
                            dateFromMillis = null
                            dateToMillis = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.reset_period))
                    }
                }

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.apply), fontSize = 16.sp)
                }
            }
        }
    }

    // Календарь выбора периода
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateRangePickerState.selectedStartDateMillis?.let {
                            dateFromMillis = it
                        }
                        dateRangePickerState.selectedEndDateMillis?.let {
                            dateToMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.select))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        stringResource(R.string.select_period),
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                        fontWeight = FontWeight.Bold
                    )
                },
                headline = {
                    val startLabel = dateRangePickerState.selectedStartDateMillis
                        ?.let { dateFormat.format(Date(it)) }
                        ?: stringResource(R.string.date_start)
                    val endLabel = dateRangePickerState.selectedEndDateMillis
                        ?.let { dateFormat.format(Date(it)) }
                        ?: stringResource(R.string.date_end)
                    Text(
                        "$startLabel — $endLabel",
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                showModeToggle = false,
                modifier = Modifier.height(500.dp)
            )
        }
    }
}

@Composable
fun FilterChipRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label) },
            leadingIcon = if (selected) {
                {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}