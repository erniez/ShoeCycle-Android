package com.shoecycle.ui.ftu

import androidx.compose.runtime.MutableState
import com.shoecycle.data.repository.FTURepository
import com.shoecycle.domain.FTUHintManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * State for FTU hint display
 */
data class FTUState(
    val currentHint: FTUHintManager.HintKey? = null,
    val showHint: Boolean = false,
    val hintMessage: String = ""
)

/**
 * Interactor for managing FTU hint display and completion
 */
class FTUInteractor(
    private val ftuRepository: FTURepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    
    sealed class Action {
        object CheckForHints : Action()
        object ShowNextHint : Action()
        object DismissHint : Action()
        object CompleteHint : Action()
        data class ShowSpecificHint(val hintKey: FTUHintManager.HintKey) : Action()
    }
    
    private val hintManager = FTUHintManager()
    
    fun handle(state: MutableState<FTUState>, action: Action) {
        when (action) {
            is Action.CheckForHints -> {
                scope.launch {
                    val nextHint = ftuRepository.nextHintFlow.first()
                    if (nextHint != null) {
                        state.value = state.value.copy(
                            currentHint = nextHint,
                            hintMessage = hintManager.getHintMessage(nextHint),
                            showHint = false // Don't show automatically, wait for ShowNextHint
                        )
                    }
                }
            }
            
            is Action.ShowNextHint -> {
                if (state.value.currentHint != null) {
                    state.value = state.value.copy(showHint = true)
                }
            }
            
            is Action.DismissHint -> {
                state.value = state.value.copy(showHint = false)
            }
            
            is Action.CompleteHint -> {
                val currentHint = state.value.currentHint
                if (currentHint != null) {
                    scope.launch {
                        ftuRepository.completeHint(currentHint)
                        // Check for next hint
                        val nextHint = ftuRepository.nextHintFlow.first()
                        state.value = if (nextHint != null) {
                            FTUState(
                                currentHint = nextHint,
                                hintMessage = hintManager.getHintMessage(nextHint),
                                showHint = false
                            )
                        } else {
                            FTUState() // No more hints
                        }
                    }
                }
            }
            
            is Action.ShowSpecificHint -> {
                scope.launch {
                    val completedHints = ftuRepository.completedHintsFlow.first()
                    if (!completedHints.contains(action.hintKey.key)) {
                        state.value = FTUState(
                            currentHint = action.hintKey,
                            hintMessage = hintManager.getHintMessage(action.hintKey),
                            showHint = true
                        )
                    }
                }
            }
        }
    }
}