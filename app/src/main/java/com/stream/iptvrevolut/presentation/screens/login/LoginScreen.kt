package com.stream.iptvrevolut.presentation.screens.login

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.stream.iptvrevolut.presentation.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val isDark = isSystemInDarkTheme()
    
    // Colores Adaptativos Cinematic Glass
    val glassColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)
    val textColor = if (isDark) Color.White else Color(0xFF1A1C1E)

    LaunchedEffect(viewModel.loginSuccess) {
        if (viewModel.loginSuccess) onLoginSuccess()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo Cinematográfico
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark) 
                            listOf(Color(0xFF0F172A), Color(0xFF000000)) 
                        else 
                            listOf(Color(0xFFE2E8F0), Color(0xFFF8FAFC))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudQueue,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "ALPHA REVOLUT",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = textColor,
                letterSpacing = 2.sp
            )
            Text(
                text = "Configura tu servidor IPTV",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = glassColor,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Campo Nombre del Perfil (Nuevo)
                    LoginTextField(
                        value = viewModel.name,
                        onValueChange = { viewModel.name = it }, // El nombre puede tener espacios
                        label = "Nombre del Perfil",
                        placeholder = "Ej: Mi Servidor",
                        icon = Icons.Default.Badge,
                        textColor = textColor,
                        imeAction = ImeAction.Next,
                        onAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo URL
                    LoginTextField(
                        value = viewModel.url,
                        onValueChange = { viewModel.url = it.sanitize() },
                        label = "URL del Servidor",
                        placeholder = "http://legazy.us:8880",
                        icon = Icons.Default.Link,
                        textColor = textColor,
                        imeAction = ImeAction.Next,
                        onAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo Usuario
                    LoginTextField(
                        value = viewModel.username,
                        onValueChange = { viewModel.username = it.sanitize() },
                        label = "Usuario",
                        placeholder = "Tu usuario",
                        icon = Icons.Default.Person,
                        textColor = textColor,
                        imeAction = ImeAction.Next,
                        onAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo Contraseña
                    var passwordVisible by remember { mutableStateOf(false) }
                    LoginTextField(
                        value = viewModel.password,
                        onValueChange = { viewModel.password = it.sanitize() },
                        label = "Contraseña",
                        placeholder = "••••••••",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        textColor = textColor,
                        imeAction = ImeAction.Done,
                        onAction = { 
                            focusManager.clearFocus()
                            viewModel.onLoginClick()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (viewModel.isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = { 
                        focusManager.clearFocus()
                        viewModel.onLoginClick() 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("CONECTAR AHORA", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }

            viewModel.errorResId?.let { resId ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(resId),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(100.dp)) // Espacio para scroll sobre teclado
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    textColor: Color,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: () -> Unit = {},
    imeAction: ImeAction,
    onAction: () -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = textColor.copy(alpha = 0.3f)) },
            leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
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
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(onAny = { onAction() }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = textColor.copy(alpha = 0.1f),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            singleLine = true
        )
    }
}

fun String.sanitize(): String {
    return this.replace("\\s".toRegex(), "").trim()
}
