package com.mistyislet.app.ui.visitors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mistyislet.app.R

private data class DeliveryMethodOption(
    val key: String,
    val labelRes: Int,
    val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVisitorSheet(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (visitor: String, method: String, hours: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var visitorName by remember { mutableStateOf("") }
    var selectedDurationIndex by remember { mutableIntStateOf(2) }
    var selectedMethod by remember { mutableStateOf("whatsapp") }

    val durations = listOf(4, 8, 24, 48, 72)
    val durationLabels = listOf("4h", "8h", "24h", "48h", "72h")
    val methods = remember {
        listOf(
            DeliveryMethodOption("email", R.string.delivery_email, Icons.Default.Email),
            DeliveryMethodOption("email_qr", R.string.delivery_email_qr, Icons.Default.QrCode),
            DeliveryMethodOption("whatsapp", R.string.delivery_whatsapp, Icons.AutoMirrored.Filled.Chat),
            DeliveryMethodOption("whatsapp_qr", R.string.delivery_whatsapp_qr, Icons.Default.QrCode),
            DeliveryMethodOption("sms", R.string.delivery_sms, Icons.Default.Sms),
        )
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isCreating) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.create_visitor_pass),
                style = MaterialTheme.typography.headlineSmall,
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = visitorName,
                onValueChange = { visitorName = it },
                label = { Text(stringResource(R.string.visitor_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isCreating,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.delivery_method),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Column(modifier = Modifier.selectableGroup()) {
                methods.forEach { method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedMethod == method.key,
                                onClick = { if (!isCreating) selectedMethod = method.key },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedMethod == method.key,
                            onClick = null,
                            enabled = !isCreating,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = method.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(method.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.valid_duration),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                durationLabels.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedDurationIndex == index,
                        onClick = { selectedDurationIndex = index },
                        shape = SegmentedButtonDefaults.itemShape(index, durationLabels.size),
                        enabled = !isCreating,
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onCreate(visitorName, selectedMethod, durations[selectedDurationIndex]) },
                modifier = Modifier.fillMaxWidth(),
                enabled = visitorName.isNotBlank() && !isCreating,
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.create_visitor_pass))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
