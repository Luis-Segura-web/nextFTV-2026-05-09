package com.stream.nextftv.presentation.screens.profileselector

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.stream.nextftv.R
import com.stream.nextftv.domain.model.ServerProfile
import com.stream.nextftv.presentation.theme.StreamingBackground
import com.stream.nextftv.presentation.theme.spacing

@Composable
private fun ProfileSelectorBackground(isDark: Boolean) {
    StreamingBackground(isDark = isDark)
}

@Composable
private fun SelectorHeader(
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color(0xFF111827)
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.66f) else Color(0xFF475569)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = stringResource(R.string.profiles_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Administra tus accesos y elige con cuál deseas ingresar.",
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryTextColor
        )
    }
}

@Composable
private fun EmptyProfilesState(isDark: Boolean, onAddNewProfile: () -> Unit) {
    val panelColor = if (isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.9f)
    val panelBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(alpha = 0.06f)
    val textColor = if (isDark) Color.White else Color(0xFF111827)
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.66f) else Color(0xFF475569)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = panelColor,
        border = BorderStroke(1.dp, panelBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.profiles_empty),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Crea tu primer acceso para comenzar a reproducir contenido.",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onAddNewProfile,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar perfil")
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ServerProfile,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    isDark: Boolean
) {
    val panelColor = if (isDark) {
        Color(0xFF101826)
    } else {
        Color(0xFFF7F9FC)
    }
    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.16f) else Color(0xFFCBD5E1)
    val secondaryTextColor = if (isDark) Color(0xFFA8B3C7) else Color(0xFF64748B)
    val iconContainerColor = if (isDark) {
        Color(0xFF22324D)
    } else {
        Color(0xFFEAF1FF)
    }
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFF0F172A).copy(alpha = 0.06f)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = panelColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = iconContainerColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = profile.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(dividerColor)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (profile.isActive) {
                    Text(
                        text = "Perfil actual",
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryTextColor
                    )
                } else {
                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("Usar perfil")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar perfil",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar")
                    }

                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.profiles_delete_confirm),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilesList(
    profiles: List<ServerProfile>,
    isDark: Boolean,
    onProfileSelected: (ServerProfile) -> Unit,
    onDelete: (ServerProfile) -> Unit,
    onEditProfile: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        contentPadding = PaddingValues(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(profiles, key = { it.id }) { profile ->
            ProfileCard(
                profile = profile,
                onClick = { onProfileSelected(profile) },
                onDelete = { onDelete(profile) },
                onEdit = { onEditProfile(profile.id) },
                isDark = isDark
            )
        }
    }
}

@Composable
fun ProfileSelectorScreen(
    onProfileSelected: () -> Unit,
    onAddNewProfile: () -> Unit,
    onEditProfile: (Int) -> Unit,
    viewModel: ProfileSelectorViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(viewModel.profileSelected) {
        if (viewModel.profileSelected) onProfileSelected()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ProfileSelectorBackground(isDark = isDark)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                SelectorHeader(
                    isDark = isDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (profiles.isEmpty()) {
                    EmptyProfilesState(
                        isDark = isDark,
                        onAddNewProfile = onAddNewProfile
                    )
                } else {
                    ProfilesList(
                        profiles = profiles,
                        isDark = isDark,
                        onProfileSelected = viewModel::onProfileClick,
                        onDelete = viewModel::onDeleteClick,
                        onEditProfile = onEditProfile
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAddNewProfile,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 24.dp, bottom = 24.dp),
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Nuevo perfil",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
