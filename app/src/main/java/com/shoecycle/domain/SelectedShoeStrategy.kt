package com.shoecycle.domain

import android.util.Log
import com.shoecycle.data.UserSettingsData
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class SelectedShoeStrategy(
    private val shoeRepository: IShoeRepository,
    private val userSettingsRepository: UserSettingsRepository
) {
    companion object {
        private const val TAG = "SelectedShoeStrategy"
    }

    suspend fun updateSelectedShoe() {
        val activeShoes = shoeRepository.getActiveShoes().first()
        
        if (activeShoes.isEmpty()) {
            Log.d(TAG, "No active shoes available, clearing selected shoe")
            userSettingsRepository.updateSelectedShoeId(null)
            return
        }

        val currentSelectedShoeId = userSettingsRepository.userSettingsFlow.firstOrNull()?.selectedShoeId

        // If we have a selected shoe ID, verify it's still in active shoes
        currentSelectedShoeId?.let { selectedId ->
            val selectedShoe = activeShoes.firstOrNull { it.id == selectedId }
            if (selectedShoe != null) {
                Log.d(TAG, "Selected shoe ${selectedShoe.brand} is still active")
                return
            } else {
                Log.d(TAG, "Previously selected shoe (ID: $selectedId) is no longer active")
                selectFirstActiveShoe(activeShoes)
            }
        } ?: run {
            Log.d(TAG, "No shoe currently selected")
            selectFirstActiveShoe(activeShoes)
        }
    }

    private suspend fun selectFirstActiveShoe(activeShoes: List<Shoe>) {
        if (activeShoes.isNotEmpty()) {
            val firstShoe = activeShoes.first()
            Log.d(TAG, "Selecting first active shoe: ${firstShoe.brand} (ID: ${firstShoe.id})")
            userSettingsRepository.updateSelectedShoeId(firstShoe.id)
        }
    }

    suspend fun selectShoe(shoeId: String) {
        val shoe = shoeRepository.getShoeByIdOnce(shoeId)
        if (shoe != null && shoe.isActive) {
            Log.d(TAG, "Manually selecting shoe: ${shoe.brand} (ID: $shoeId)")
            userSettingsRepository.updateSelectedShoeId(shoeId)
        } else {
            Log.w(TAG, "Attempted to select inactive or non-existent shoe: $shoeId")
            updateSelectedShoe()
        }
    }

    suspend fun getSelectedShoe(): Shoe? {
        val selectedShoeId = userSettingsRepository.userSettingsFlow.firstOrNull()?.selectedShoeId
        return selectedShoeId?.let { shoeRepository.getShoeByIdOnce(it) }
    }
}