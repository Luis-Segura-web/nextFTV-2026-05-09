package com.stream.nextftv.presentation.screens.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.stream.nextftv.data.remote.tmdb.TmdbMovieShortDto
import com.stream.nextftv.presentation.theme.spacing

private fun resolvePosterModel(path: String?): String? {
    if (path.isNullOrBlank()) return null
    return if (path.startsWith("http://") || path.startsWith("https://")) {
        path
    } else {
        "https://image.tmdb.org/t/p/w342$path"
    }
}

@Composable
fun HorizontalListSection(title: String, content: LazyListScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 16.dp),
            content = content
        )
    }
}

@Composable
fun MovieShortItem(
    movie: TmdbMovieShortDto,
    isActual: Boolean = false,
    isFound: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column(modifier = Modifier.width(100.dp).clickable(enabled = isFound) { onClick() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = resolvePosterModel(movie.posterPath),
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (isFound) 1f else 0.4f
            )

            if (isActual) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp),
                    color = Color.Red,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "ACTUAL",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else if (!isFound) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "NO DISPONIBLE",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = movie.title ?: movie.tvName ?: "",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
            color = if (isFound) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            }
        )
    }
}

@Composable
fun HorizontalSeriesListSection(title: String, content: LazyListScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 24.dp, horizontal = MaterialTheme.spacing.medium)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
fun SeriesShortItem(
    series: TmdbMovieShortDto,
    isActual: Boolean = false,
    isFound: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp).clickable(enabled = isFound) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = resolvePosterModel(series.posterPath),
                contentDescription = series.title ?: series.tvName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (isFound) 1f else 0.4f
            )

            if (isActual) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp),
                    color = Color.Red,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "ACTUAL",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else if (!isFound) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "NO DISPONIBLE",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = series.tvName ?: series.title ?: "",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
            color = if (isFound) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            }
        )
    }
}
