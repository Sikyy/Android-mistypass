package com.mistyislet.app.ui.admin.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mistyislet.app.ui.components.MistySegmentedControl

@Composable
fun AdminTabPicker(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    MistySegmentedControl(
        labels = tabs,
        selectedIndex = selectedIndex,
        onSelected = onTabSelected,
        modifier = modifier.fillMaxWidth(),
    )
}
