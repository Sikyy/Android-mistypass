package com.mistyislet.app.ui.visitors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.mistyislet.app.R
import com.mistyislet.app.ui.components.MistyFormSheet
import com.mistyislet.app.ui.components.MistyFormTextField
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistySegmentedControl

private data class DeliveryMethodOption(
    val key: String,
    val labelRes: Int,
    val icon: ImageVector,
)

/**
 * delivery_method values accepted by the backend's normalizeDeliveryMethod
 * (api/internal/modules/access/service_policies.go). Any other value is
 * rejected with HTTP 400, so the picker must offer only these. Kept as plain
 * strings (no Compose types) so the contract can be unit-tested directly.
 */
internal val supportedDeliveryMethodKeys: List<String> = listOf("email_qr", "wallet")

/** Default picker selection; must be one of [supportedDeliveryMethodKeys]. */
internal const val defaultDeliveryMethod: String = "email_qr"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVisitorSheet(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (visitor: String, method: String, hours: Int) -> Unit,
) {
    var visitorName by remember { mutableStateOf("") }
    var selectedDurationIndex by remember { mutableIntStateOf(2) }
    var selectedMethod by remember { mutableStateOf(defaultDeliveryMethod) }

    val durations = listOf(4, 8, 24, 48, 72)
    val durationLabels = durations.map { hours -> stringResource(R.string.visitors_hours, hours) }
    val methods = remember {
        supportedDeliveryMethodKeys.map { key ->
            when (key) {
                "wallet" -> DeliveryMethodOption(key, R.string.delivery_wallet, Icons.Default.AccountBalanceWallet)
                else -> DeliveryMethodOption(key, R.string.delivery_email_qr, Icons.Default.QrCode)
            }
        }
    }

    MistyFormSheet(
        title = stringResource(R.string.visitors_new),
        cancelLabel = stringResource(R.string.cancel),
        confirmLabel = stringResource(R.string.admin_create),
        onCancel = { if (!isCreating) onDismiss() },
        onConfirm = { onCreate(visitorName, selectedMethod, durations[selectedDurationIndex]) },
        confirmEnabled = visitorName.isNotBlank() && !isCreating,
    ) {
        item {
            MistyGroupedSection(title = stringResource(R.string.guest_section_visitor)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    MistyFormTextField(
                        value = visitorName,
                        onValueChange = { visitorName = it },
                        label = stringResource(R.string.visitor_name),
                    )
                }
            }
        }
        item {
            MistyGroupedSection(title = stringResource(R.string.delivery_method)) {
                methods.forEachIndexed { index, method ->
                    DeliveryMethodRow(
                        method = method,
                        selected = selectedMethod == method.key,
                        enabled = !isCreating,
                        onClick = { selectedMethod = method.key },
                    )
                    if (index < methods.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp, end = 16.dp))
                    }
                }
            }
        }
        item {
            MistyGroupedSection(title = stringResource(R.string.valid_duration)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    MistySegmentedControl(
                        labels = durationLabels,
                        selectedIndex = selectedDurationIndex,
                        onSelected = { if (!isCreating) selectedDurationIndex = it },
                    )
                }
            }
        }
        item {
            MistyGroupedSection(title = stringResource(R.string.visitors_notes)) {
                Text(
                    text = stringResource(R.string.visitors_notes_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
        if (isCreating) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.create_visitor_pass),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryMethodRow(
    method: DeliveryMethodOption,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = method.icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(method.labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
