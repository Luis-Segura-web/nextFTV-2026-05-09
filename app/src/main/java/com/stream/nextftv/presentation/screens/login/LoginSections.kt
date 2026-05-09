package com.stream.nextftv.presentation.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stream.nextftv.R
import com.stream.nextftv.presentation.theme.StreamingBackground
import com.stream.nextftv.presentation.theme.spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LoginContent(
    name: String,
    url: String,
    username: String,
    password: String,
    isLoading: Boolean,
    isEditMode: Boolean,
    hasSavedProfiles: Boolean,
    errorText: String?,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onBackToProfiles: () -> Unit,
    focusManager: FocusManager
) {
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()
    val panelColor = if (isDark) Color(0xFF101826).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.92f)
    val panelBorder = if (isDark) Color.White.copy(alpha = 0.18f) else Color(0xFF0F172A).copy(alpha = 0.12f)
    val textColor = if (isDark) Color.White else Color(0xFF1A1C1E)
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.64f) else Color(0xFF334155).copy(alpha = 0.78f)
    val fieldContainerColor = if (isDark) Color(0xFF141F31) else Color.White
    val fieldBorderColor = if (isDark) Color.White.copy(alpha = 0.16f) else Color(0xFF0F172A).copy(alpha = 0.12f)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFF0F172A).copy(alpha = 0.05f)
    var showContent by remember { mutableStateOf(false) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    var contentHeight by remember { mutableIntStateOf(0) }
    val imeVisible = WindowInsets.isImeVisible
    val shouldScroll = imeVisible || (viewportHeight > 0 && contentHeight > viewportHeight)

    val brandScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.975f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "login_brand_scale"
    )
    val brandAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.8f,
        animationSpec = tween(durationMillis = 500),
        label = "login_brand_alpha"
    )

    LaunchedEffect(Unit) {
        showContent = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        StreamingBackground(isDark = isDark)

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(700)) + slideInVertically(
                initialOffsetY = { it / 12 },
                animationSpec = tween(700, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .onSizeChanged { viewportHeight = it.height }
                    .then(if (shouldScroll) Modifier.verticalScroll(scrollState) else Modifier)
                    .padding(MaterialTheme.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .onSizeChanged { contentHeight = it.height },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        modifier = Modifier.graphicsLayer {
                            scaleX = brandScale
                            scaleY = brandScale
                            alpha = brandAlpha
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = panelColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, panelBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.login_icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "NEXTFTV PLAYER",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = textColor,
                                    letterSpacing = 0.4.sp
                                )
                                Text(
                                    text = "Conexión a proveedor",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryTextColor,
                                    letterSpacing = 0.2.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    var passwordVisible by remember { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = panelColor,
                        border = androidx.compose.foundation.BorderStroke(1.15.dp, panelBorder)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                            Text(
                                text = if (isEditMode) "Editar Perfil" else "Ingresar Datos de Acceso",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            LoginTextField(
                                value = name,
                                onValueChange = onNameChange,
                                label = "Nombre de Perfil",
                                placeholder = "Ej: Sala principal",
                                icon = Icons.Default.Badge,
                                textColor = textColor,
                                fieldContainerColor = fieldContainerColor,
                                fieldBorderColor = fieldBorderColor,
                                isDark = isDark,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                                onAction = { focusManager.moveFocus(FocusDirection.Down) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            LoginTextField(
                                value = url,
                                onValueChange = onUrlChange,
                                label = "Servidor",
                                placeholder = "https://tu-panel.com:puerto",
                                icon = Icons.Default.Link,
                                textColor = textColor,
                                fieldContainerColor = fieldContainerColor,
                                fieldBorderColor = fieldBorderColor,
                                isDark = isDark,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                                onAction = { focusManager.moveFocus(FocusDirection.Down) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            LoginTextField(
                                value = username,
                                onValueChange = onUsernameChange,
                                label = "Usuario",
                                placeholder = "Usuario del panel",
                                icon = Icons.Default.Person,
                                textColor = textColor,
                                fieldContainerColor = fieldContainerColor,
                                fieldBorderColor = fieldBorderColor,
                                isDark = isDark,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                                onAction = { focusManager.moveFocus(FocusDirection.Down) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            LoginTextField(
                                value = password,
                                onValueChange = onPasswordChange,
                                label = "Contraseña",
                                placeholder = "••••••••",
                                icon = Icons.Default.Lock,
                                textColor = textColor,
                                fieldContainerColor = fieldContainerColor,
                                fieldBorderColor = fieldBorderColor,
                                isDark = isDark,
                                isPassword = true,
                                passwordVisible = passwordVisible,
                                onTogglePassword = { passwordVisible = !passwordVisible },
                                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                                onAction = {
                                    focusManager.clearFocus()
                                    onLoginClick()
                                }
                            )

                            if (errorText != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.24f)
                                    )
                                ) {
                                    Text(
                                        text = errorText,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = dividerColor)
                            Spacer(modifier = Modifier.height(14.dp))

                            LoginActionButton(
                                isLoading = isLoading,
                                isDark = isDark,
                                isEditMode = isEditMode,
                                onClick = {
                                    focusManager.clearFocus()
                                    onLoginClick()
                                }
                            )

                            if (hasSavedProfiles) {
                                Spacer(modifier = Modifier.height(8.dp))
                                ProfilesListButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        onBackToProfiles()
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}
