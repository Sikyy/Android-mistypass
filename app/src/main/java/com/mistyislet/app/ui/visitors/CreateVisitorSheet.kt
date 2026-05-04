package com.mistyislet.app.ui.visitors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mistyislet.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVisitorSheet(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (visitor: String, method: String, hours: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var visitorName by remember { mutableStateOf("") }
    var visitorPhone by remember { mutableStateOf("") }
    var hostName by remember { mutableStateOf("") }
    var selectedDurationIndex by remember { mutableIntStateOf(2) } // default 24h
    var selectedMethod by remember { mutableStateOf("email") }
    var methodExpanded by remember { mutableStateOf(false) }

    val durations = listOf(4, 8, 24, 48, 72)
    val durationLabels = listOf("4h", "8h", "24h", "48h", "72h")
    val methods = listOf("email" to "Email", "email_qr" to "Email + QR", "sms" to "SMS")

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

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = visitorPhone,
                onValueChange = { visitorPhone = it },
                label = { Text(stringResource(R.string.visitor_phone)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isCreating,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = hostName,
                onValueChange = { hostName = it },
                label = { Text(stringResource(R.string.visitor_host)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isCreating,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Duration segmented button
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

            Spacer(modifier = Modifier.height(16.dp))

            // Delivery method dropdown
            ExposedDropdownMenuBox(
                expanded = methodExpanded,
                onExpandedChange = { if (!isCreating) methodExpanded = it },
            ) {
                OutlinedTextField(
                    value = methods.first { it.first == selectedMethod }.second,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.delivery_method)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                    methods.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { selectedMethod = value; methodExpanded = false },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val contact = visitorPhone.ifBlank { visitorName }
                    onCreate(contact, selectedMethod, durations[selectedDurationIndex])
                },
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
