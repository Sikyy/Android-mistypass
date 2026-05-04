package com.mistyislet.app.ui.doors

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mistyislet.app.R
import com.mistyislet.app.ui.theme.Danger
import com.mistyislet.app.ui.theme.Success
import kotlinx.coroutines.delay

@Composable
fun UnlockResultDialog(
    state: UnlockDialogState,
    onDismiss: () -> Unit,
) {
    val bgColor = when (state) {
        is UnlockDialogState.Loading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
        is UnlockDialogState.Granted -> Success.copy(alpha = 0.95f)
        is UnlockDialogState.Denied -> Danger.copy(alpha = 0.95f)
    }

    // Auto-dismiss on success
    if (state is UnlockDialogState.Granted) {
        LaunchedEffect(Unit) {
            delay(2000)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    if (state !is UnlockDialogState.Loading) onDismiss()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (state) {
                is UnlockDialogState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color.White,
                        strokeWidth = 4.dp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.door_unlocking),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                }
                is UnlockDialogState.Granted -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.door_access_granted),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.doorName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
                is UnlockDialogState.Denied -> {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.door_unlock_denied),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.reason,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
        }
    }
}

sealed class UnlockDialogState {
    data object Loading : UnlockDialogState()
    data class Granted(val doorName: String) : UnlockDialogState()
    data class Denied(val reason: String) : UnlockDialogState()
}
