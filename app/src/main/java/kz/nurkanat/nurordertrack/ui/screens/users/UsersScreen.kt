package kz.nurkanat.nurordertrack.ui.screens.users

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kz.nurkanat.nurordertrack.R
import kz.nurkanat.nurordertrack.data.model.User
import kz.nurkanat.nurordertrack.data.model.UserRole
import kz.nurkanat.nurordertrack.di.AppContainer
import kz.nurkanat.nurordertrack.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(navController: NavHostController) {
    val userRepo = AppContainer.userRepository
    val users by userRepo.getAllUsersFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // ── текущий залогиненный пользователь (для защиты от самодеактивации) ──
    val currentUserId = remember { userRepo.getCurrentUserId() }

    var showSheet by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<User?>(null) }

    // ── поиск / фильтр ──
    var searchQuery by remember { mutableStateOf("") }
    var filterRole by remember { mutableStateOf<UserRole?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    // ── фильтрация списка ──
    val filteredUsers = remember(users, searchQuery, filterRole) {
        users.filter { user ->
            val matchesSearch = searchQuery.isBlank() ||
                    user.name.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true)
            val matchesRole = filterRole == null || user.role == filterRole
            matchesSearch && matchesRole
        }
    }

    // ── разбиваем по ролям ──
    val admins    = filteredUsers.filter { it.role == UserRole.ADMIN }
    val managers  = filteredUsers.filter { it.role == UserRole.MANAGER }
    val executors = filteredUsers.filter { it.role == UserRole.EXECUTOR }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.users),
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
                    // ── кнопка фильтра по роли ──
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = "Фильтр по роли",
                                tint = if (filterRole != null)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Все роли") },
                                onClick = { filterRole = null; showFilterMenu = false },
                                leadingIcon = {
                                    if (filterRole == null)
                                        Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.role_admin)) },
                                onClick = { filterRole = UserRole.ADMIN; showFilterMenu = false },
                                leadingIcon = {
                                    if (filterRole == UserRole.ADMIN)
                                        Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.role_manager)) },
                                onClick = { filterRole = UserRole.MANAGER; showFilterMenu = false },
                                leadingIcon = {
                                    if (filterRole == UserRole.MANAGER)
                                        Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.role_executor)) },
                                onClick = { filterRole = UserRole.EXECUTOR; showFilterMenu = false },
                                leadingIcon = {
                                    if (filterRole == UserRole.EXECUTOR)
                                        Icon(Icons.Filled.Check, contentDescription = null)
                                }
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
            FloatingActionButton(
                onClick = { editingUser = null; showSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Filled.PersonAdd,
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
        ) {
            // ── строка поиска ──
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск по имени или email...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (filteredUsers.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Group,
                    title = stringResource(R.string.no_users),
                    subtitle = stringResource(R.string.no_users_hint)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // ── секция Администраторы ──
                    if (admins.isNotEmpty()) {
                        item {
                            RoleSectionHeader(
                                label = stringResource(R.string.role_admin),
                                count = admins.size,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        items(admins, key = { it.id }) { user ->
                            UserItem(
                                user = user,
                                isSelf = user.id == currentUserId,
                                onEdit = { editingUser = user; showSheet = true },
                                onToggleActive = {
                                    // ── ЗАЩИТА: нельзя деактивировать самого себя ──
                                    if (user.id != currentUserId) {
                                        scope.launch {
                                            userRepo.deactivateUser(user.id, !user.isActive)
                                        }
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // ── секция Менеджеры ──
                    if (managers.isNotEmpty()) {
                        item {
                            RoleSectionHeader(
                                label = stringResource(R.string.role_manager),
                                count = managers.size,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(managers, key = { it.id }) { user ->
                            UserItem(
                                user = user,
                                isSelf = false,
                                onEdit = { editingUser = user; showSheet = true },
                                onToggleActive = {
                                    scope.launch {
                                        userRepo.deactivateUser(user.id, !user.isActive)
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // ── секция Исполнители ──
                    if (executors.isNotEmpty()) {
                        item {
                            RoleSectionHeader(
                                label = stringResource(R.string.role_executor),
                                count = executors.size,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        items(executors, key = { it.id }) { user ->
                            UserItem(
                                user = user,
                                isSelf = false,
                                onEdit = { editingUser = user; showSheet = true },
                                onToggleActive = {
                                    scope.launch {
                                        userRepo.deactivateUser(user.id, !user.isActive)
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
        UserSheet(
            user = editingUser,
            onDismiss = { showSheet = false },
            onSave = { name, email, password, role ->
                scope.launch {
                    if (editingUser == null) {
                        userRepo.createUser(name, email, password, role)
                    } else {
                        userRepo.updateUser(editingUser!!.id, name, role)
                    }
                }
                showSheet = false
            }
        )
    }
}

// ── Заголовок секции ──────────────────────────────────────────────────────────
@Composable
fun RoleSectionHeader(
    label: String,
    count: Int,
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
            Text(
                text = "$label ($count)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = color.copy(alpha = 0.25f),
            thickness = 1.dp
        )
    }
}

// ── Карточка сотрудника ───────────────────────────────────────────────────────
@Composable
fun UserItem(
    user: User,
    isSelf: Boolean,          // текущий залогиненный пользователь?
    onEdit: () -> Unit,
    onToggleActive: () -> Unit
) {
    val roleColor = when (user.role) {
        UserRole.ADMIN    -> MaterialTheme.colorScheme.error
        UserRole.MANAGER  -> MaterialTheme.colorScheme.primary
        UserRole.EXECUTOR -> MaterialTheme.colorScheme.tertiary
    }

    val roleLabel = when (user.role) {
        UserRole.ADMIN    -> stringResource(R.string.role_admin)
        UserRole.MANAGER  -> stringResource(R.string.role_manager)
        UserRole.EXECUTOR -> stringResource(R.string.role_executor)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!user.isActive)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = roleColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            user.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = roleColor,
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            user.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        // ── бейджик "Это я" ──
                        if (isSelf) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "Вы",
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        if (!user.isActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    stringResource(R.string.inactive),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    Text(
                        user.email,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = roleColor.copy(alpha = 0.1f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            roleLabel,
                            fontSize = 11.sp,
                            color = roleColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.edit)
                    )
                }
                // ── кнопка деактивации скрыта для самого себя ──
                if (!isSelf) {
                    IconButton(onClick = onToggleActive) {
                        Icon(
                            if (user.isActive) Icons.Filled.PersonOff
                            else Icons.Filled.PersonAdd,
                            contentDescription = if (user.isActive)
                                stringResource(R.string.deactivate)
                            else
                                stringResource(R.string.activate),
                            tint = if (user.isActive)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    // пустой placeholder чтобы карточка не "прыгала" по высоте
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

// ── Bottom Sheet добавления/редактирования ────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSheet(
    user: User?,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, password: String, role: UserRole) -> Unit
) {
    val roles = listOf(
        UserRole.EXECUTOR to stringResource(R.string.role_executor),
        UserRole.MANAGER  to stringResource(R.string.role_manager),
        UserRole.ADMIN    to stringResource(R.string.role_admin)
    )

    var name            by remember { mutableStateOf(user?.name ?: "") }
    var email           by remember { mutableStateOf(user?.email ?: "") }
    var password        by remember { mutableStateOf("") }
    var selectedRole    by remember { mutableStateOf(user?.role ?: UserRole.EXECUTOR) }
    var showRoleDropdown by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    var nameError     by remember { mutableStateOf(false) }
    var emailError    by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    val isEditing = user != null

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(if (isEditing) R.string.edit_user else R.string.new_user),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text(stringResource(R.string.name)) },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                isError = nameError,
                supportingText = { if (nameError) Text(stringResource(R.string.required_field)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (!isEditing) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; emailError = false },
                    label = { Text(stringResource(R.string.email)) },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    isError = emailError,
                    supportingText = { if (emailError) Text(stringResource(R.string.invalid_email)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = false },
                    label = { Text(stringResource(R.string.password)) },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.Visibility
                                else Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible)
                        androidx.compose.ui.text.input.VisualTransformation.None
                    else
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    isError = passwordError,
                    supportingText = { if (passwordError) Text(stringResource(R.string.min_password)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            ExposedDropdownMenuBox(
                expanded = showRoleDropdown,
                onExpandedChange = { showRoleDropdown = it }
            ) {
                OutlinedTextField(
                    value = roles.find { it.first == selectedRole }?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.role)) },
                    leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRoleDropdown)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showRoleDropdown,
                    onDismissRequest = { showRoleDropdown = false }
                ) {
                    roles.forEach { (role, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { selectedRole = role; showRoleDropdown = false }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    nameError     = name.isBlank()
                    emailError    = !isEditing && !email.contains("@")
                    passwordError = !isEditing && password.length < 6

                    if (!nameError && !emailError && !passwordError) {
                        onSave(name.trim(), email.trim(), password, selectedRole)
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