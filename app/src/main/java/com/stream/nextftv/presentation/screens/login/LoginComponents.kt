package com.stream.nextftv.presentation.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ProfilesListButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
    ) {
        Text(
            text = "Volver a perfiles",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
internal fun LoginActionButton(
    isLoading: Boolean,
    isDark: Boolean,
    isEditMode: Boolean,
    onClick: () -> Unit
) {
    val ctaScale by animateFloatAsState(
        targetValue = if (isLoading) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "login_cta_scale"
    )
    if (isLoading) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    } else {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .graphicsLayer {
                    scaleX = ctaScale
                    scaleY = ctaScale
                },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (isDark) 8.dp else 4.dp,
                pressedElevation = 2.dp,
                hoveredElevation = if (isDark) 10.dp else 6.dp,
                focusedElevation = if (isDark) 10.dp else 6.dp
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isEditMode) "Guardar cambios" else "Guardar y entrar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.1.sp
                )
            }
        }
    }
}

@Composable
internal fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    textColor: Color,
    fieldContainerColor: Color,
    fieldBorderColor: Color,
    isDark: Boolean,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: () -> Unit = {},
    imeAction: ImeAction,
    onAction: () -> Unit
) {
    val iconContainerColor = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    val strongerBorderColor = if (isDark) {
        fieldBorderColor.copy(alpha = 0.95f)
    } else {
        fieldBorderColor.copy(alpha = 1f)
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = iconContainerColor,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor.copy(alpha = 0.84f)
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = fieldContainerColor
            ),
            border = BorderStroke(1.2.dp, strongerBorderColor)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                placeholder = { Text(placeholder, color = textColor.copy(alpha = 0.34f)) },
                trailingIcon = {
                    if (isPassword) {
                        IconButton(onClick = onTogglePassword) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = textColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                },
                visualTransformation = if (isPassword && !passwordVisible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(onAny = { onAction() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedPlaceholderColor = textColor.copy(alpha = 0.28f),
                    unfocusedPlaceholderColor = textColor.copy(alpha = 0.28f)
                ),
                singleLine = true,
                maxLines = 1
            )
        }
    }
}
