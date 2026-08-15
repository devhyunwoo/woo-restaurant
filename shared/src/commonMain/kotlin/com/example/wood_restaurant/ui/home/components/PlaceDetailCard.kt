package com.example.wood_restaurant.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.wood_restaurant.domain.Restaurant
import com.example.wood_restaurant.ui.map.markerColor

/**
 * 마커/행을 탭했을 때 지도 아래쪽에 떠오르는 상세 카드.
 * 모달이 아니라서 카드가 떠 있어도 지도는 계속 만질 수 있다.
 */
@Composable
fun PlaceDetailCard(
    place: Restaurant,
    isFavorite: Boolean,
    onClose: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCallClick: () -> Unit,
    onDirectionsClick: () -> Unit,
    onOpenLinkClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f).padding(top = 6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(place.category.markerColor, CircleShape),
                        )
                        Text(
                            text = place.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = listOfNotNull(
                            place.subCategory.takeIf { it.isNotBlank() },
                            place.distanceLabel,
                            place.walkingLabel,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RatingLine(place, modifier = Modifier.padding(top = 2.dp))
                    if (place.displayAddress.isNotBlank()) {
                        Text(
                            text = place.displayAddress,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ActionItem(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    label = if (isFavorite) "찜됨" else "찜",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else null,
                    onClick = onFavoriteClick,
                )
                ActionItem(
                    icon = Icons.Filled.Call,
                    label = "전화",
                    enabled = place.telephone.isNotBlank(),
                    onClick = onCallClick,
                )
                ActionItem(icon = Icons.Filled.Place, label = "길찾기", onClick = onDirectionsClick)
                ActionItem(icon = Icons.Filled.Info, label = "네이버", onClick = onOpenLinkClick)
                ActionItem(icon = Icons.Filled.Share, label = "공유", onClick = onShareClick)
            }
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color? = null,
) {
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant
        tint != null -> tint
        else -> MaterialTheme.colorScheme.primary
    }
    Column(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = contentColor)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}
