package com.stream.nextftv.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ParentalControlTopBar(
    canChangePin: Boolean,
    onBack: () -> Unit,
    onChangePin: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Control Parental",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            if (canChangePin) {
                IconButton(onClick = onChangePin) {
                    Icon(Icons.Default.Password, contentDescription = "Cambiar NIP")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
internal fun PinGate(
    modifier: Modifier,
    hasPin: Boolean,
    error: String?,
    onUnlock: (String) -> Unit,
    onCreatePin: (String, String) -> Unit,
    onClearError: () -> Unit
) {
    var pin = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var confirmPin = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (hasPin) "Ingresa NIP de 4 dígitos" else "Crea NIP de 4 dígitos",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        PinTextField(
            value = pin.value,
            label = if (hasPin) "NIP" else "Nuevo NIP",
            onValueChange = {
                pin.value = it
                if (error != null) onClearError()
            }
        )
        if (!hasPin) {
            Spacer(modifier = Modifier.height(10.dp))
            PinTextField(
                value = confirmPin.value,
                label = "Confirmar NIP",
                onValueChange = {
                    confirmPin.value = it
                    if (error != null) onClearError()
                }
            )
        }
        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = {
                if (hasPin) onUnlock(pin.value) else onCreatePin(pin.value, confirmPin.value)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (hasPin) "Ingresar" else "Crear NIP")
        }
    }
}

@Composable
internal fun ParentalControlContent(
    selectedTab: Int,
    tabs: List<String>,
    selectedItems: List<ParentalCategoryUiItem>,
    selectedType: String,
    innerPadding: PaddingValues,
    onSelectTab: (Int) -> Unit,
    onCategoryVisibilityChange: (String, String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .imePadding()
    ) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onSelectTab(index) },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedItems.isEmpty()) {
            Text(
                text = "No hay categorías disponibles",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                item {
                    Text(
                        text = "Desactiva una categoría para ocultar todo su contenido en Todos, Recientes y Favoritos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(selectedItems, key = { "${selectedType}_${it.categoryId}" }) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.categoryName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (category.isHidden) "Oculta" else "Visible",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (category.isHidden) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                        Switch(
                            checked = !category.isHidden,
                            onCheckedChange = { isVisible ->
                                onCategoryVisibilityChange(
                                    selectedType,
                                    category.categoryId,
                                    isVisible
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
