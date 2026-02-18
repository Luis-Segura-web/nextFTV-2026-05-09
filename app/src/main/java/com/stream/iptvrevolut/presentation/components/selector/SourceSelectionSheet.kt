package com.stream.iptvrevolut.presentation.components.selector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SourceSelectionSheet(
    title: String,
    sources: List<T>,
    onSourceClick: (T) -> Unit,
    onDismiss: () -> Unit,
    sourceName: (T) -> String,
    sourceCategory: (T) -> String? = { null }
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(16.dp)
            )
            
            Text(
                text = "Varias versiones encontradas en tu catálogo:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )

            LazyColumn {
                items(sources) { source ->
                    ListItem(
                        headlineContent = { 
                            Text(
                                text = sourceName(source),
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        supportingContent = { 
                            sourceCategory(source)?.let { 
                                Text(text = it, color = MaterialTheme.colorScheme.primary) 
                            } 
                        },
                        trailingContent = { 
                            Icon(Icons.Default.PlayArrow, contentDescription = null) 
                        },
                        modifier = Modifier.clickable { onSourceClick(source) },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}
