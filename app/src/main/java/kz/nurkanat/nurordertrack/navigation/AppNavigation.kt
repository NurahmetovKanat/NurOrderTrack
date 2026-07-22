package kz.nurkanat.nurordertrack.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import kz.nurkanat.nurordertrack.R
import kz.nurkanat.nurordertrack.data.model.UserRole
import kz.nurkanat.nurordertrack.ui.screens.SplashScreen
import kz.nurkanat.nurordertrack.ui.screens.analytics.AnalyticsScreen
import kz.nurkanat.nurordertrack.ui.screens.auth.LoginScreen
import kz.nurkanat.nurordertrack.ui.screens.clients.ClientsScreen
import kz.nurkanat.nurordertrack.ui.screens.dashboard.DashboardScreen
import kz.nurkanat.nurordertrack.ui.screens.orders.CreateOrderScreen
import kz.nurkanat.nurordertrack.ui.screens.orders.EditOrderScreen
import kz.nurkanat.nurordertrack.ui.screens.orders.OrderDetailScreen
import kz.nurkanat.nurordertrack.ui.screens.orders.OrdersScreen
import kz.nurkanat.nurordertrack.ui.screens.products.ProductsScreen
import kz.nurkanat.nurordertrack.ui.screens.profile.ProfileScreen
import kz.nurkanat.nurordertrack.ui.screens.users.UsersScreen

data class BottomNavItem(
    val screen: Screen,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    isLoggedIn: Boolean,
    userRole: UserRole,
    onLoginSuccess: () -> Unit
) {
    val startDestination = Screen.Splash.route

    val bottomNavItems = buildList {
        add(BottomNavItem(Screen.Dashboard, Icons.Filled.Home,
            stringResource(R.string.dashboard)))
        add(BottomNavItem(Screen.Orders, Icons.Filled.List,
            stringResource(R.string.orders)))
        if (userRole == UserRole.ADMIN || userRole == UserRole.MANAGER) {
            add(BottomNavItem(Screen.Clients, Icons.Filled.People,
                stringResource(R.string.clients)))
            add(BottomNavItem(Screen.Analytics, Icons.Filled.BarChart,
                stringResource(R.string.analytics)))
        }
        add(BottomNavItem(Screen.Profile, Icons.Filled.Person,
            stringResource(R.string.profile)))
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = isLoggedIn && bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(onFinished = {
                    val nextRoute = if (isLoggedIn) Screen.Dashboard.route
                    else Screen.Login.route
                    navController.navigate(nextRoute) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.Login.route) {
                LoginScreen(onLoginSuccess = {
                    onLoginSuccess()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(navController = navController)
            }

            composable(Screen.Orders.route) {
                OrdersScreen(navController = navController, userRole = userRole)
            }

            composable(Screen.OrderDetail.route) { backStack ->
                val orderId = backStack.arguments?.getString("orderId") ?: return@composable
                OrderDetailScreen(
                    orderId = orderId,
                    navController = navController,
                    userRole = userRole
                )
            }

            composable(Screen.CreateOrder.route) {
                CreateOrderScreen(navController = navController)
            }

            composable(Screen.EditOrder.route) { backStack ->
                val orderId = backStack.arguments?.getString("orderId") ?: return@composable
                EditOrderScreen(orderId = orderId, navController = navController)
            }

            composable(Screen.Clients.route) {
                ClientsScreen(navController = navController, userRole = userRole)
            }

            composable(Screen.Products.route) {
                ProductsScreen(navController = navController)
            }

            composable(Screen.Users.route) {
                UsersScreen(navController = navController)
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(navController = navController, userRole = userRole)
            }

            composable(Screen.Profile.route) {
                ProfileScreen(navController = navController, onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }
        }
    }
}