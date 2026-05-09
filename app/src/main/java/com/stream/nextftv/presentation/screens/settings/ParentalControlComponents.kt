package com.stream.nextftv.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun ChangePinDialog(
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
internal fun PinTextField(
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
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
