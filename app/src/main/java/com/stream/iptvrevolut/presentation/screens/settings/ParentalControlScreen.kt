package com.stream.iptvrevolut.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalControlScreen(
    onBack: () -> Unit,
    viewModel: ParentalControlViewModel = hiltViewModel()
) {
    val live by viewModel.liveCategories.collectAsStateWithLifecycle()
    val movies by viewModel.movieCategories.collectAsStateWithLifecycle()
    val series by viewModel.seriesCategories.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    val tabs = listOf("TV en Vivo", "Películas", "Series")

    Scaffold(
        topBar = {
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
                    if (viewModel.isUnlocked && viewModel.hasPin) {
                        IconButton(onClick = { showChangePinDialog = true }) {
                            Icon(Icons.Default.Password, contentDescription = "Cambiar NIP")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (!viewModel.isUnlocked) {
            PinGate(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                hasPin = viewModel.hasPin,
                error = viewModel.pinError,
                onUnlock = { pin -> viewModel.unlockWithPin(pin) },
                onCreatePin = { pin, confirm -> viewModel.createPin(pin, confirm) },
                onClearError = viewModel::clearError
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val selectedType = when (selectedTab) {
                0 -> "live"
                1 -> "vod"
                else -> "series"
            }
            val selectedItems = when (selectedTab) {
                0 -> live
                1 -> movies
                else -> series
            }

            if (selectedItems.isEmpty()) {
                Text(
                    text = "No hay categorías disponibles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                                    viewModel.onCategoryVisibilityChange(
                                        contentType = selectedType,
                                        categoryId = category.categoryId,
                                        visible = isVisible
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showChangePinDialog) {
            ChangePinDialog(
                onDismiss = {
                    showChangePinDialog = false
                    viewModel.clearError()
                },
                error = viewModel.pinError,
                onConfirm = { current, new, confirm ->
                    viewModel.changePin(current, new, confirm) {
                        showChangePinDialog = false
                    }
                }
            )
        }
    }
}

@Composable
private fun PinGate(
    modifier: Modifier,
    hasPin: Boolean,
    error: String?,
    onUnlock: (String) -> Unit,
    onCreatePin: (String, String) -> Unit,
    onClearError: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

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
            value = pin,
            label = if (hasPin) "NIP" else "Nuevo NIP",
            onValueChange = {
                pin = it
                if (error != null) onClearError()
            }
        )
        if (!hasPin) {
            Spacer(modifier = Modifier.height(10.dp))
            PinTextField(
                value = confirmPin,
                label = "Confirmar NIP",
                onValueChange = {
                    confirmPin = it
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
                if (hasPin) onUnlock(pin) else onCreatePin(pin, confirmPin)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (hasPin) "Ingresar" else "Crear NIP")
        }
    }
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    error: String?,
    onConfirm: (current: String, new: String, confirm: String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar NIP") },
        text = {
            Column {
                PinTextField(
                    value = current,
                    label = "NIP actual",
                    onValueChange = { current = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                PinTextField(
                    value = newPin,
                    label = "Nuevo NIP",
                    onValueChange = { newPin = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                PinTextField(
                    value = confirm,
                    label = "Confirmar nuevo NIP",
                    onValueChange = { confirm = it }
                )
                if (!error.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current, newPin, confirm) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun PinTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            onValueChange(new.filter { it.isDigit() }.take(4))
        },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}
