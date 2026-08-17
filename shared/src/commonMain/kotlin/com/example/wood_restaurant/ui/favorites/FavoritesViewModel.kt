package com.example.wood_restaurant.ui.favorites

import androidx.lifecycle.ViewModel
import com.example.wood_restaurant.data.FavoritesRepository
import com.example.wood_restaurant.domain.PlaceCategory
import com.example.wood_restaurant.domain.Restaurant
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

data class FavoritesState(
    val favorites: List<Restaurant> = emptyList(),
    /** null이면 전체. */
    val categoryFilter: PlaceCategory? = null,
) {
    val visible: List<Restaurant>
        get() = categoryFilter?.let { c -> favorites.filter { it.category == c } } ?: favorites

    val countByCategory: Map<PlaceCategory, Int>
        get() = favorites.groupingBy { it.category }.eachCount()
}

sealed interface FavoritesSideEffect {
    data class ShowMessage(val message: String) : FavoritesSideEffect
}

class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
) : ViewModel(), ContainerHost<FavoritesState, FavoritesSideEffect> {

    override val container = container<FavoritesState, FavoritesSideEffect>(
        FavoritesState(favorites = favoritesRepository.favorites.value),
        onCreate = {
            favoritesRepository.favorites.collect { list ->
                reduce { state.copy(favorites = list) }
            }
        },
    )

    fun onCategoryFilterSelected(category: PlaceCategory?) = intent {
        reduce { state.copy(categoryFilter = category) }
    }

    fun onRemove(place: Restaurant) = intent {
        if (favoritesRepository.isFavorite(place.id)) {
            favoritesRepository.toggle(place)
            postSideEffect(FavoritesSideEffect.ShowMessage("${place.name} 찜 해제"))
        }
    }

    fun onClearAll() = intent {
        val count = state.favorites.size
        if (count == 0) return@intent
        favoritesRepository.clear()
        postSideEffect(FavoritesSideEffect.ShowMessage("찜 ${count}개를 모두 지웠어요"))
    }
}
