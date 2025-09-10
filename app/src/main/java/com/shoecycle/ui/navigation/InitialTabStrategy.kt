package com.shoecycle.ui.navigation

import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.ui.ShoeCycleDestination
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class InitialTabStrategy(
    private val shoeRepository: IShoeRepository
) {
    fun initialTab(): String {
        // Using runBlocking here is acceptable for initial app setup
        // This matches iOS behavior where the initial tab is determined synchronously
        return runBlocking {
            val activeShoes = shoeRepository.getActiveShoes().first()
            if (activeShoes.isNotEmpty()) {
                ShoeCycleDestination.AddDistance.route
            } else {
                ShoeCycleDestination.ActiveShoes.route
            }
        }
    }
}