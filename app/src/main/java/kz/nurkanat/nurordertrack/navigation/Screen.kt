package kz.nurkanat.nurordertrack.navigation

sealed class Screen(val route: String) {
    // Auth
    object Login : Screen("login")

    // Main
    object Dashboard : Screen("dashboard")
    object Orders : Screen("orders")
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: String) = "order_detail/$orderId"
    }
    object CreateOrder : Screen("create_order")
    object EditOrder : Screen("edit_order/{orderId}") {
        fun createRoute(orderId: String) = "edit_order/$orderId"
    }

    // Clients
    object Clients : Screen("clients")

    // Products
    object Products : Screen("products")

    // Users
    object Users : Screen("users")

    // Analytics
    object Analytics : Screen("analytics")

    // Profile
    object Profile : Screen("profile")
    object Splash : Screen("splash")
}