package com.example.wood_restaurant.ui.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wood_restaurant.domain.MinRating
import com.example.wood_restaurant.domain.PlaceCategory
import com.example.wood_restaurant.domain.PlaceFilter
import com.example.wood_restaurant.domain.SearchRadius
import com.example.wood_restaurant.domain.SortOption

/**
 * 홈 상단 필터 바.
 *
 * 축이 서로 독립적이라 원하는 만큼 겹쳐 걸 수 있다.
 * 카테고리(다중) · 정렬 · 반경 · 최소 별점 — 전부 AND로 적용된다.
 */
@Composable
fun FilterBar(
    filter: PlaceFilter,
    onCategoryToggled: (PlaceCategory) -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onRadiusSelected: (SearchRadius) -> Unit,
    onMinRatingSelected: (MinRating) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlaceCategory.entries.forEach { category ->
            val selected = category in filter.categories
            FilterChip(
                selected = selected,
                onClick = { onCategoryToggled(category) },
                label = { Text("${category.emoji} ${category.label}") },
                leadingIcon = if (selected) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(0.dp)) }
                } else {
                    null
                },
            )
        }

        DropdownFilterChip(
            label = filter.sort.label,
            selected = filter.sort != SortOption.DISTANCE,
            options = SortOption.entries,
            optionLabel = { it.label },
            isSelected = { it == filter.sort },
            onSelected = onSortSelected,
        )

        DropdownFilterChip(
            label = "반경 ${filter.radius.label}",
            selected = filter.radius != SearchRadius.R_1000,
            options = SearchRadius.entries,
            optionLabel = { it.label },
            isSelected = { it == filter.radius },
            onSelected = onRadiusSelected,
        )

        DropdownFilterChip(
            label = if (filter.minRating == MinRating.ANY) "별점" else "★ ${filter.minRating.label}",
            selected = filter.minRating != MinRating.ANY,
            options = MinRating.entries,
            optionLabel = { it.label },
            isSelected = { it == filter.minRating },
            onSelected = onMinRatingSelected,
        )

        if (filter.activeCount > 0) {
            FilterChip(
                selected = false,
                onClick = onReset,
                label = { Text("초기화 (${filter.activeCount})") },
            )
        }
    }
}

/** 탭하면 목록이 열리는 단일 선택 칩. */
@Composable
private fun <T> DropdownFilterChip(
    label: String,
    selected: Boolean,
    options: List<T>,
    optionLabel: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    // 메뉴가 칩 바로 아래에 뜨도록 같은 Box에 둔다.
    Box {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            },
            shape = FilterChipDefaults.shape,
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                    trailingIcon = if (isSelected(option)) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
