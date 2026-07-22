package kz.nurkanat.nurordertrack.di

import kz.nurkanat.nurordertrack.data.repository.ClientRepository
import kz.nurkanat.nurordertrack.data.repository.OrderRepository
import kz.nurkanat.nurordertrack.data.repository.ProductRepository
import kz.nurkanat.nurordertrack.data.repository.UserRepository

// Простой DI контейнер без Hilt
object AppContainer {
    val userRepository by lazy { UserRepository() }
    val clientRepository by lazy { ClientRepository() }
    val productRepository by lazy { ProductRepository() }
    val orderRepository by lazy { OrderRepository() }
}