package com.example.wood_restaurant.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.wood_restaurant.domain.Restaurant

/** 별점/리뷰수 한 줄. 데이터가 없으면(=RatingSource가 null을 주면) 그 사실을 그대로 보여준다. */
@Composable
fun RatingLine(place: Restaurant, modifier: Modifier = Modifier) {
    val rating = place.rating
    val reviewCount = place.reviewCount

    if (rating == null && reviewCount == null) {
        Text(
            text = "평점 정보 없음",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = modifier,
        )
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
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
internal fun formatRating(value: Double): String {
    val tenths = (value * 10).toInt()
    return "${tenths / 10}.${tenths % 10}"
}
