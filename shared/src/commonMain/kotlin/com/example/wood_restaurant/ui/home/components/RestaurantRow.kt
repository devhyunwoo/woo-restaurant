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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.wood_restaurant.domain.Restaurant
import com.example.wood_restaurant.ui.map.markerColor

/** 목록 한 줄. 지도 마커와 같은 카테고리 색 점을 앞에 둬서 눈으로 대응이 되게 한다. */
@Composable
fun RestaurantRow(
    place: Restaurant,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(place.category.markerColor, CircleShape),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        place.subCategory.takeIf { it.isNotBlank() },
                        place.roadAddress.takeIf { it.isNotBlank() } ?: place.address,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                RatingLine(place)
            }

            Text(
                text = place.distanceLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** 별점/리뷰수. 데이터가 없으면(=RatingSource가 null을 주면) 그 사실을 그대로 보여준다. */
@Composable
private fun RatingLine(place: Restaurant) {
    val rating = place.rating
    val reviewCount = place.reviewCount

    if (rating == null && reviewCount == null) {
        Text(
            text = "평점 정보 없음",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Color(0xFFF59F00),
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = rating?.let { formatRating(it) } ?: "–",
            style = MaterialTheme.typography.labelMedium,
        )
        if (reviewCount != null) {
            Text(
                text = "리뷰 $reviewCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 소수점 한 자리 고정. KMP 공통 코드에는 String.format이 없다. */
private fun formatRating(value: Double): String {
    val tenths = (value * 10).toInt()
    return "${tenths / 10}.${tenths % 10}"
}
