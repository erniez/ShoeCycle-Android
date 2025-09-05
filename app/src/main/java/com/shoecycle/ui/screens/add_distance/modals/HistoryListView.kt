package com.shoecycle.ui.screens.add_distance.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IHistoryRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.DistanceUtility
import com.shoecycle.domain.models.Shoe
import com.shoecycle.ui.theme.shoeCycleOrange
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.shoecycle.domain.FileProviderUtility
import java.io.File

@Composable
fun HistoryListView(
    shoe: Shoe,
    shoeRepository: IShoeRepository,
    historyRepository: IHistoryRepository,
    userSettingsRepository: UserSettingsRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state = remember { mutableStateOf(HistoryListState()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val interactor = remember {
        HistoryListInteractor(
            shoeRepository = shoeRepository,
            historyRepository = historyRepository,
            userSettingsRepository = userSettingsRepository
        )
    }
    
    // Load user settings for distance unit
    val distanceUnit = remember {
        runBlocking {
            userSettingsRepository.userSettingsFlow.firstOrNull()?.distanceUnit ?: DistanceUnit.MILES
        }
    }
    
    LaunchedEffect(shoe) {
        android.util.Log.d("HistoryListView", "LaunchedEffect triggered for shoe ${shoe.id}: ${shoe.displayName}")
        interactor.handle(state = state, action = HistoryListInteractor.Action.ViewAppeared, shoe = shoe)
    }
    
    // Handle Snackbar display
    LaunchedEffect(state.value.showUndoSnackbar) {
        if (state.value.showUndoSnackbar) {
            val result = snackbarHostState.showSnackbar(
                message = "Run deleted",
                actionLabel = "UNDO",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    interactor.handle(state, HistoryListInteractor.Action.UndoDelete, shoe)
                }
                SnackbarResult.Dismissed -> {
                    // Just dismiss the snackbar, deletion already happened
                    interactor.handle(state, HistoryListInteractor.Action.DismissSnackbar, shoe)
                }
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            HistoryListContent(
                state = state.value,
                shoe = shoe,
                distanceUnit = distanceUnit,
                snackbarHostState = snackbarHostState,
                onAction = { action ->
                    interactor.handle(state, action, shoe)
                },
                onDismiss = onDismiss
            )
        }
    }
    
    if (state.value.showExportDialog) {
        ExportDialog(
            onDismiss = {
                interactor.handle(state, HistoryListInteractor.Action.DismissExport, shoe)
            },
            onExport = {
                interactor.handle(state, HistoryListInteractor.Action.ExportToCSV(context), shoe)
            }
        )
    }
    
    // Handle successful export - launch email intent
    LaunchedEffect(state.value.exportFilePath) {
        state.value.exportFilePath?.let { filePath ->
            val csvFile = File(filePath)
            if (csvFile.exists()) {
                val fileProviderUtility = FileProviderUtility()
                
                // Check if email client is available
                if (!fileProviderUtility.canSendEmail(context)) {
                    // Fall back to general share intent
                    try {
                        val shareIntent = fileProviderUtility.createShareIntent(context, csvFile)
                        context.startActivity(shareIntent)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            message = "No app available to share the CSV file",
                            duration = androidx.compose.material3.SnackbarDuration.Long
                        )
                    }
                } else {
                    // Use email intent
                    try {
                        val emailIntent = fileProviderUtility.createEmailIntent(
                            context = context,
                            csvFile = csvFile,
                            shoeBrand = shoe.brand.ifEmpty { "Unknown" }
                        )
                        context.startActivity(emailIntent)
                    } catch (e: Exception) {
                        // Fallback to general share if email fails
                        try {
                            val shareIntent = fileProviderUtility.createShareIntent(context, csvFile)
                            context.startActivity(shareIntent)
                        } catch (shareException: Exception) {
                            snackbarHostState.showSnackbar(
                                message = "Failed to share the CSV file",
                                duration = androidx.compose.material3.SnackbarDuration.Long
                            )
                        }
                    }
                }
                interactor.handle(state, HistoryListInteractor.Action.DismissExport, shoe)
            }
        }
    }
    
    // Handle export error
    LaunchedEffect(state.value.exportError) {
        state.value.exportError?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = androidx.compose.material3.SnackbarDuration.Long
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryListContent(
    state: HistoryListState,
    shoe: Shoe,
    distanceUnit: DistanceUnit,
    snackbarHostState: SnackbarHostState,
    onAction: (HistoryListInteractor.Action) -> Unit,
    onDismiss: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        // Header with buttons
        TopAppBar(
            title = { Text("Run History") },
            navigationIcon = {
                TextButton(onClick = { onAction(HistoryListInteractor.Action.ShowExport) }) {
                    Text("Export")
                }
            },
            actions = {
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        )
        
        // YTD display
        val ytd = state.yearlyTotals[state.currentYear] ?: 0.0
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("YTD: ")
                    }
                    append(DistanceUtility.displayString(ytd, distanceUnit))
                    append(" ${DistanceUtility.getUnitLabel(distanceUnit)}")
                },
                style = MaterialTheme.typography.titleMedium,
                color = shoeCycleOrange
            )
        }
        
        // Column headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Run Date",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Distance (${DistanceUtility.getUnitLabel(distanceUnit)})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        HorizontalDivider()
        
        // History list
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                state.sections.forEach { section ->
                    item {
                        HistorySectionHeader(
                            section = section,
                            distanceUnit = distanceUnit
                        )
                    }
                    
                    itemsIndexed(
                        items = section.historyViewModels,
                        key = { _, item -> item.id }
                    ) { index, historyViewModel ->
                        HistoryListItem(
                            viewModel = historyViewModel,
                            distanceUnit = distanceUnit,
                            onDelete = {
                                onAction(HistoryListInteractor.Action.RemoveHistory(section, index))
                            }
                        )
                    }
                    
                    if (section.yearlyRunTotal != null) {
                        item {
                            YearlyTotalRow(
                                yearTotal = section.yearlyRunTotal!!,
                                distanceUnit = distanceUnit
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun HistorySectionHeader(
    section: HistorySectionViewModel,
    distanceUnit: DistanceUnit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                append("Total for ${section.monthString}: ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(DistanceUtility.displayString(section.runTotal, distanceUnit))
                    append(" ${DistanceUtility.getUnitLabel(distanceUnit)}")
                }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = shoeCycleOrange
        )
    }
}

@Composable
private fun YearlyTotalRow(
    yearTotal: Double,
    distanceUnit: DistanceUnit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Total for the year: ${DistanceUtility.displayString(yearTotal, distanceUnit)} ${DistanceUtility.getUnitLabel(distanceUnit)}",
            style = MaterialTheme.typography.bodyLarge,
            color = shoeCycleOrange,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export History") },
        text = { Text("Export run history data as CSV file and send via email?") },
        confirmButton = {
            TextButton(onClick = onExport) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}