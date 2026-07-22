package kz.nurkanat.nurordertrack.ui.screens.profile

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kz.nurkanat.nurordertrack.data.model.UserRole
import kz.nurkanat.nurordertrack.di.AppContainer
import kz.nurkanat.nurordertrack.navigation.Screen
import kz.nurkanat.nurordertrack.utils.LanguageHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController, onLogout: () -> Unit) {
    val userRepo = AppContainer.userRepository
    val currentUser by userRepo.getCurrentUserFlow()
        .collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current

    var showLogoutDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember {
        mutableStateOf(LanguageHelper.getSavedLanguage(context))
    }

    val roleLabel = when (currentUser?.role) {
        UserRole.ADMIN -> stringResource(R.string.role_admin)
        UserRole.MANAGER -> stringResource(R.string.role_manager)
        UserRole.EXECUTOR -> stringResource(R.string.role_executor)
        null -> "—"
    }

    val roleColor = when (currentUser?.role) {
        UserRole.ADMIN -> MaterialTheme.colorScheme.error
        UserRole.MANAGER -> MaterialTheme.colorScheme.primary
        UserRole.EXECUTOR -> MaterialTheme.colorScheme.tertiary
        null -> MaterialTheme.colorScheme.onSurface
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.profile),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Аватар и имя
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = roleColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    currentUser?.name?.take(1)?.uppercase() ?: "?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    color = roleColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                currentUser?.name ?: "—",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                currentUser?.email ?: "—",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = roleColor.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    roleLabel,
                                    fontSize = 12.sp,
                                    color = roleColor,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp, vertical = 4.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Аккаунт
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.account),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        ProfileInfoRow(
                            icon = Icons.Filled.Person,
                            label = stringResource(R.string.name_label),
                            value = currentUser?.name ?: "—"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileInfoRow(
                            icon = Icons.Filled.Email,
                            label = stringResource(R.string.email_label),
                            value = currentUser?.email ?: "—"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileInfoRow(
                            icon = Icons.Filled.Badge,
                            label = stringResource(R.string.role_label),
                            value = roleLabel
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileInfoRow(
                            icon = Icons.Filled.Circle,
                            label = stringResource(R.string.status_label),
                            value = if (currentUser?.isActive == true)
                                stringResource(R.string.active)
                            else
                                stringResource(R.string.inactive)
                        )
                    }
                }
            }

            // Управление для Админа
            if (currentUser?.role == UserRole.ADMIN) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                stringResource(R.string.management),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(
                                    start = 8.dp, top = 8.dp, bottom = 4.dp
                                )
                            )
                            ListItem(
                                headlineContent = {
                                    Text(stringResource(R.string.users))
                                },
                                supportingContent = {
                                    Text(stringResource(R.string.manage_users))
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Filled.ManageAccounts,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingContent = {
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                                },
                                modifier = Modifier.clickable {
                                    navController.navigate(Screen.Users.route)
                                }
                            )
                            HorizontalDivider()
                            ListItem(
                                headlineContent = {
                                    Text(stringResource(R.string.products))
                                },
                                supportingContent = {
                                    Text(stringResource(R.string.products_catalog))
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Filled.Inventory,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingContent = {
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                                },
                                modifier = Modifier.clickable {
                                    navController.navigate(Screen.Products.route)
                                }
                            )
                        }
                    }
                }
            }

            // Язык — заголовок намеренно хардкодим на трёх языках
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Язык / Тіл / Language",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "ru" to "Рус",
                                "kk" to "Қаз",
                                "en" to "Eng"
                            ).forEach { (code, label) ->
                                val isSelected = currentLanguage == code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        currentLanguage = code
                                        LanguageHelper.setLocale(context, code)
                                        (context as? Activity)?.recreate()
                                    },
                                    label = {
                                        Text(
                                            label,
                                            fontWeight = if (isSelected)
                                                FontWeight.Bold
                                            else
                                                FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Кнопка выхода
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            MaterialTheme.colorScheme.error
                        )
                    )
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.logout), fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Filled.Logout, contentDescription = null) },
            title = { Text(stringResource(R.string.logout_title)) },
            text = { Text(stringResource(R.string.logout_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        userRepo.signOut()
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(
                        stringResource(R.string.logout_short),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            "$label:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(80.dp)
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}