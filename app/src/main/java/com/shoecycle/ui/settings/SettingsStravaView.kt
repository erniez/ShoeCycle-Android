package com.shoecycle.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shoecycle.R
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.strava.StravaTokenKeeper
import com.shoecycle.ui.auth.StravaAuthActivity
import com.shoecycle.ui.theme.shoeCycleOrange

@Composable
fun SettingsStravaView(
    tokenKeeper: StravaTokenKeeper,
    userSettingsRepository: UserSettingsRepository
) {
    val state = remember { mutableStateOf(StravaState()) }
    val interactor = remember { StravaInteractor(tokenKeeper, userSettingsRepository) }
    val context = LocalContext.current
    
    // Set up activity result launcher for OAuth
    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        interactor.handleActivityResult(
            state = state,
            resultCode = result.resultCode,
            data = result.data
        )
    }
    
    // Set the launcher in the interactor
    LaunchedEffect(authLauncher) {
        interactor.setAuthLauncher(authLauncher)
    }
    
    // Check connection status on view appear
    LaunchedEffect(Unit) {
        interactor.handle(state, StravaInteractor.Action.ViewAppeared)
    }
    
    // Show error dialog if there's an error
    state.value.error?.let { error ->
        AlertDialog(
            onDismissRequest = { 
                interactor.handle(state, StravaInteractor.Action.DismissError)
            },
            title = { Text("Strava Connection Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(
                    onClick = { 
                        interactor.handle(state, StravaInteractor.Action.DismissError)
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(shoeCycleOrange)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_strava_name_logo),
                    contentDescription = "Strava",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentScale = ContentScale.FillHeight,
                    alignment = Alignment.CenterStart
                )
                
                if (state.value.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    if (state.value.isConnected) {
                        FilledTonalButton(
                            onClick = {
                                interactor.handle(state, StravaInteractor.Action.DisconnectClicked)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect")
                        }
                    } else {
                        Button(
                            onClick = {
                                interactor.handle(
                                    state, 
                                    StravaInteractor.Action.ConnectClicked,
                                    context
                                )
                            }
                        ) {
                            Text("Connect")
                        }
                    }
                }
            }
            
            if (state.value.isConnected) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Your activities will be automatically uploaded to Strava",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}